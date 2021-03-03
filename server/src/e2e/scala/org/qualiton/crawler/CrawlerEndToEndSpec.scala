package org.qualiton.crawler

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

import scala.concurrent.duration._

import cats.data.{ NonEmptyList, OptionT }
import cats.effect.{ ContextShift, IO }
import cats.scalatest.{ EitherMatchers, ValidatedValues }
import cats.syntax.flatMap._
import fs2.concurrent.SignallingRef

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.{ Uri, Url }
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import io.chrisdavenport.log4cats.Logger
import io.circe.{ HCursor, Json }
import io.circe.parser.parse
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, KamonConfig, PublisherConfig, SlackConfig }
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.common.logging.LoggingIO
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ AuthorPersistence, CommentPersistence, DiscussionAggregateRootPersistence, DiscussionPersistence, TeamPersistence }
import org.qualiton.crawler.server.config.ServiceConfig
import org.qualiton.crawler.server.main.Server
import org.qualiton.crawler.testsupport.dockerkit.PostgresDockerTestKit
import org.qualiton.crawler.testsupport.scalatest.GithubApiV3MockServerSupport
import org.qualiton.crawler.testsupport.wiremock.GithubApiV3MockServer
import org.qualiton.slack.testsupport.scalatest.{ SlackApiMockServerSupport, SlackRtmApiMockServerSupport }
import org.qualiton.slack.testsupport.wiremock.SlackApiMockServer

class CrawlerEndToEndSpec
  extends FeatureSpec
    with GivenWhenThen
    with Matchers
    with EitherValues
    with EitherMatchers
    with OptionValues
    with ValidatedValues
    with Eventually
    with TypeCheckedTripleEquals
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GithubApiV3MockServerSupport
    with SlackApiMockServerSupport
    with SlackRtmApiMockServerSupport
    with PostgresDockerTestKit
    with Inside
    with LoggingIO {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(10, Seconds),
      interval = Span(100, Millis))

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val es = Executors.newCachedThreadPool()
  implicit val timer = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetTables()
    slackApiMockServer.resetRequests()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Server.fromConfig(IO.pure(appConfig)).interruptWhen(stopSignal).compile.drain.unsafeRunAsyncAndForget()
  }

  override def afterAll(): Unit = {
    (stopSignal.set(true) >> dataSource.close).unsafeRunSync()
    super.afterAll()
  }

  val appHttpPort: UserPortNumber = 9000

  val appName: NonEmptyString = "test-github-discussion-crawler"

  val appConfig =
    ServiceConfig(
      httpPort = appHttpPort,
      gitConfig =
        GitConfig(
          baseUrl = refineV[Url](s"http://localhost:${ GithubApiV3MockServer.GithubApiV3Port }").getOrElse(throw new IllegalArgumentException),
          requestTimeout = 5.seconds,
          apiToken = config.Secret(refineV[NonEmpty](GithubApiV3MockServer.testApiToken).getOrElse(throw new IllegalArgumentException)),
          refreshInterval = 1.second),
      publisherConfig =
        PublisherConfig(
          slackConfig =
            SlackConfig(
              baseUrl = refineV[Url](s"http://localhost:${ SlackApiMockServer.SlackApiPort }").getOrElse(throw new IllegalArgumentException),
              requestTimeout = 5.seconds,
              pingInterval = 5.seconds,
              apiToken = config.Secret(SlackApiMockServer.testApiToken),
              defaultChannelName = "default_channel"),
          enableNotificationPublish = true,
          ignoreEarlierThan = 6.hours),
      databaseConfig =
        DatabaseConfig(
          databaseDriverName = "org.postgresql.Driver",
          jdbcUrl = refineV[Uri](s"jdbc:postgresql://localhost:$postgresAdvertisedPort/postgres").getOrElse(throw new IllegalArgumentException),
          username = "postgres",
          password = config.Secret("postgres"),
          maximumPoolSize = 5),
      kamonConfig =
        KamonConfig(applicationName = appName)
    )

  lazy val dataSource: DataSource[IO] = DataSource[IO](appConfig.databaseConfig, ec, ec)

  lazy val transactor: Transactor[IO] = dataSource.hikariTransactor

  private val stopSignal: SignallingRef[IO, Boolean] = SignallingRef[IO, Boolean](false).unsafeRunSync()

  feature("Crawler publishes new discussion discovered event") {
    scenario("New discussion is discovered with 0 comment and published to Slack") {
      When("When there is a new discussion with 0 comment")
      val teamId1 = 1L
      val discussionId1 = 1L
      val referenceInstant: Instant = Instant.now()
      githubApiV3MockServer.mockDiscussions(teamId1, 1, 0, referenceInstant)
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName, "targeted-channel-1")
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()

      Then("new discussion is persisted to db")
      eventually {
        val result = findDiscussionAggregateRootPersistence(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionAggregateRootPersistence(
          t@TeamPersistence(teamId, teamName, description, _, _),
          d@DiscussionPersistence(_, discussionId, title, _, _),
          NonEmptyList(c@CommentPersistence(_, _, commentId0, AuthorPersistence(authorId0, authorName0, authorAvatarUrl0), url0, body0, bodyVersion0, _, _), _))) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            description should ===("Boost team")
            t.createdAt.toString should ===("2017-05-10T14:04:30Z")
            t.updatedAt.toString should ===("2018-06-11T16:10:11Z")

            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            d.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            d.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))

            commentId0 should ===(0L)
            authorId0 should ===(5830214L)
            authorName0 should ===("lachatak")
            authorAvatarUrl0 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url0 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            body0 should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            bodyVersion0 should ===("76bc8de0a4e713e15fecba89143ac2af")
            c.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            c.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
        }
      }

      And("all the slack notifications should be sent out")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")
        requests should have size 2
      }

      And("the slack default channel should receive the publish post request with valid attachment")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")

        val request = requests.head

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.downField("channel").as[String] should beRight("CHANNEL_ID1")

        val message = cursor.downField("attachments").downArray.first

        message.downField("pretext").as[String] should beRight("New discussion has been discovered")
        message.downField("color").as[String] should beRight("good")
        message.downField("author_name").as[String] should beRight("lachatak")
        message.downField("author_icon").as[String] should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.downField("title").as[String] should beRight(s"discussion-title-$discussionId1")
        message.downField("title_link").as[String] should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")

        val fields0 = message.downField("fields").downArray.first
        fields0.downField("title").as[String] should beRight("Team")
        fields0.downField("value").as[String] should beRight("Test Team")
        fields0.downField("short").as[Boolean] should beRight(true)

        val fields1 = message.downField("fields").downArray.first.right
        fields1.downField("title").as[String] should beRight("Targeted")
        fields1.downField("value").as[String] should beRight(s"@targeted-person-$discussionId1, #targeted-channel-$discussionId1")
        fields1.downField("short").as[Boolean] should beRight(false)
      }

      And("the slack additional channel should receive the publish post request with valid attachment")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")

        val request = requests.last

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.downField("channel").as[String] should beRight("CHANNEL_ID2")

        val message = cursor.downField("attachments").downArray.first

        message.downField("pretext").as[String] should beRight("New discussion has been discovered")
        message.downField("color").as[String] should beRight("good")
        message.downField("author_name").as[String] should beRight("lachatak")
        message.downField("author_icon").as[String] should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.downField("title").as[String] should beRight(s"discussion-title-$discussionId1")
        message.downField("title_link").as[String] should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")

        val fields0 = message.downField("fields").downArray.first
        fields0.downField("title").as[String] should beRight("Team")
        fields0.downField("value").as[String] should beRight("Test Team")
        fields0.downField("short").as[Boolean] should beRight(true)

        val fields1 = message.downField("fields").downArray.first.right
        fields1.downField("title").as[String] should beRight("Targeted")
        fields1.downField("value").as[String] should beRight(s"@targeted-person-$discussionId1, #targeted-channel-$discussionId1")
        fields1.downField("short").as[Boolean] should beRight(false)
      }
    }

    scenario("New discussion is discovered with more comments and published to Slack") {
      When("When there is a new discussion discovered with 2 comments")
      val teamId1 = 1L
      val discussionId1 = 1L
      val referenceInstant: Instant = Instant.now()
      githubApiV3MockServer.mockDiscussions(teamId1, 1, 2, referenceInstant)
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName, "targeted-channel-1")
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()

      Then("new discussion is persisted to db")
      eventually {
        val result = findDiscussionAggregateRootPersistence(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionAggregateRootPersistence(
          t@TeamPersistence(teamId, teamName, description, _, _),
          d@DiscussionPersistence(_, discussionId, title, _, _),
          NonEmptyList(
          h@CommentPersistence(_, _, commentId0, AuthorPersistence(authorId0, authorName0, authorAvatarUrl0), url0, body0, bodyVersion0, _, _),
          List(
          ta1@CommentPersistence(_, _, commentId1, AuthorPersistence(authorId1, authorName1, authorAvatarUrl1), url1, body1, bodyVersion1, _, _),
          ta2@CommentPersistence(_, _, commentId2, AuthorPersistence(authorId2, authorName2, authorAvatarUrl2), url2, body2, bodyVersion2, _, _))))) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            description should ===("Boost team")
            t.createdAt.toString should ===("2017-05-10T14:04:30Z")
            t.updatedAt.toString should ===("2018-06-11T16:10:11Z")

            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            d.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            d.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))

            commentId0 should ===(0L)
            authorId0 should ===(5830214L)
            authorName0 should ===("lachatak")
            authorAvatarUrl0 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url0 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            body0 should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            bodyVersion0 should ===("76bc8de0a4e713e15fecba89143ac2af")
            h.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            h.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))

            commentId1 should ===(commentId1)
            authorId1 should ===(5830214L)
            authorName1 should ===("lachatak")
            authorAvatarUrl1 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url1 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId1")
            body1 should ===(s"comment-body-$commentId1 @targeted-person-$commentId1 #targeted-channel-$commentId1")
            bodyVersion1 should ===("abf55d3127759d1716fc9d1be3c9237c")
            ta1.createdAt should ===(referenceInstant.plus(2, ChronoUnit.HOURS))
            ta1.updatedAt should ===(referenceInstant.plus(2, ChronoUnit.HOURS))

            commentId2 should ===(commentId2)
            authorId2 should ===(5830214L)
            authorName2 should ===("lachatak")
            authorAvatarUrl2 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url2 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId2")
            body2 should ===(s"comment-body-$commentId2 @targeted-person-$commentId2 #targeted-channel-$commentId2")
            bodyVersion2 should ===("abf55d3127759d1716fc9d1be3c9237c")
            ta2.createdAt should ===(referenceInstant.plus(3, ChronoUnit.HOURS))
            ta2.updatedAt should ===(referenceInstant.plus(3, ChronoUnit.HOURS))


        }
      }

      And("all the slack notifications should be sent out")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")
        requests should have size 2
      }

      And("the slack default channel should receive the publish post request with valid attachment")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")

        val doc: Json = parse(requests.head.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.get[String]("channel") should beRight("CHANNEL_ID1")
        val message = cursor.downField("attachments").downArray.first

        message.get[String]("pretext") should beRight("New discussion has been discovered with 2 comments")
        message.get[String]("color") should beRight("good")
        message.get[String]("author_name") should beRight("lachatak")
        message.get[String]("author_icon") should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.get[String]("title") should beRight(s"discussion-title-$discussionId1")
        message.get[String]("title_link") should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")

        val field0 = message.downField("fields").downArray.first
        field0.get[String]("title") should beRight("Team")
        field0.get[String]("value") should beRight("Test Team")
        field0.get[Boolean]("short") should beRight(true)

        val field1 = message.downField("fields").downArray.first.right
        field1.get[String]("title") should beRight("Comments")
        field1.get[String]("value") should beRight("2")
        field1.get[Boolean]("short") should beRight(true)

        val fields2 = message.downField("fields").downArray.first.right.right
        fields2.downField("title").as[String] should beRight("Targeted")
        fields2.downField("value").as[String] should beRight(s"@targeted-person-$discussionId1, #targeted-channel-$discussionId1")
        fields2.downField("short").as[Boolean] should beRight(false)
      }

      And("the slack additional channel should receive the publish post request with valid attachment")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")

        val doc: Json = parse(requests.last.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.get[String]("channel") should beRight("CHANNEL_ID2")
        val message = cursor.downField("attachments").downArray.first

        message.get[String]("pretext") should beRight("New discussion has been discovered with 2 comments")
        message.get[String]("color") should beRight("good")
        message.get[String]("author_name") should beRight("lachatak")
        message.get[String]("author_icon") should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.get[String]("title") should beRight(s"discussion-title-$discussionId1")
        message.get[String]("title_link") should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")

        val field0 = message.downField("fields").downArray.first
        field0.get[String]("title") should beRight("Team")
        field0.get[String]("value") should beRight("Test Team")
        field0.get[Boolean]("short") should beRight(true)

        val field1 = message.downField("fields").downArray.first.right
        field1.get[String]("title") should beRight("Comments")
        field1.get[String]("value") should beRight("2")
        field1.get[Boolean]("short") should beRight(true)

        val fields2 = message.downField("fields").downArray.first.right.right
        fields2.downField("title").as[String] should beRight("Targeted")
        fields2.downField("value").as[String] should beRight(s"@targeted-person-$discussionId1, #targeted-channel-$discussionId1")
        fields2.downField("short").as[Boolean] should beRight(false)
      }
    }
  }

  feature("Crawler publishes new comment discovered event") {
    scenario("New comment is discovered and published to Slack") {
      Given("there is a discussion with 0 comment")
      val teamId1 = 1L
      val discussionId1 = 1L
      val commentId = 1L
      val referenceInstant: Instant = Instant.now()
      githubApiV3MockServer.mockDiscussions(teamId1, 1, 0, referenceInstant)
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName, "targeted-channel-1")
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()
      val firstUpdatedAt: Instant = eventually {
        val result = findDiscussionAggregateRootPersistence(teamId1, discussionId1)
        result.value.discussion.updatedAt
      }

      println(firstUpdatedAt)
      Then("a new comment is discovered")
      githubApiV3MockServer.mockDiscussions(teamId1, 1, 1, referenceInstant)

      Then("the new comment is persisted to db")
      eventually {
        val result = findDiscussionAggregateRootPersistence(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionAggregateRootPersistence(
          t@TeamPersistence(teamId, teamName, description, _, _),
          d@DiscussionPersistence(_, discussionId, title, _, _),
          NonEmptyList(
          h@CommentPersistence(_, _, commentId0, AuthorPersistence(authorId0, authorName0, authorAvatarUrl0), url0, body0, bodyVersion0, _, _),
          List(
          ta1@CommentPersistence(_, _, commentId1, AuthorPersistence(authorId1, authorName1, authorAvatarUrl1), url1, body1, bodyVersion1, _, _))))) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            description should ===("Boost team")
            t.createdAt.toString should ===("2017-05-10T14:04:30Z")
            t.updatedAt.toString should ===("2018-06-11T16:10:11Z")

            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            d.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            d.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))

            commentId0 should ===(0L)
            authorId0 should ===(5830214L)
            authorName0 should ===("lachatak")
            authorAvatarUrl0 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url0 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            body0 should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            bodyVersion0 should ===("76bc8de0a4e713e15fecba89143ac2af")
            h.createdAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))
            h.updatedAt should ===(referenceInstant.plus(1, ChronoUnit.HOURS))

            commentId1 should ===(commentId1)
            authorId1 should ===(5830214L)
            authorName1 should ===("lachatak")
            authorAvatarUrl1 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            url1 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId1")
            body1 should ===(s"comment-body-$commentId1 @targeted-person-$commentId1 #targeted-channel-$commentId1")
            bodyVersion1 should ===("abf55d3127759d1716fc9d1be3c9237c")
            ta1.createdAt should ===(referenceInstant.plus(2, ChronoUnit.HOURS))
            ta1.updatedAt should ===(referenceInstant.plus(2, ChronoUnit.HOURS))
        }
      }

      And("all the slack notifications should be sent out")
      eventually {
        val requests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage")
        requests should have size 4
      }

      And("the slack default channel should receive the publish post request with valid attachment")
      eventually {
        val newCommentRequests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage").drop(2)

        val doc: Json = parse(newCommentRequests.head.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.get[String]("channel") should beRight("CHANNEL_ID1")

        val message = cursor.downField("attachments").downArray.first

        message.get[String]("pretext") should beRight("New comment has been discovered")
        message.get[String]("color") should beRight("good")
        message.get[String]("author_name") should beRight("lachatak")
        message.get[String]("author_icon") should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.get[String]("title") should beRight(s"discussion-title-$discussionId1")
        message.get[String]("title_link") should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId")

        val field0 = message.downField("fields").downArray.first
        field0.get[String]("title") should beRight("Team")
        field0.get[String]("value") should beRight("Test Team")
        field0.get[Boolean]("short") should beRight(true)

        val field1 = message.downField("fields").downArray.first.right
        field1.get[String]("title") should beRight("Comments")
        field1.get[String]("value") should beRight("1")
        field1.get[Boolean]("short") should beRight(true)

        val fields2 = message.downField("fields").downArray.first.right.right
        fields2.downField("title").as[String] should beRight("Targeted")
        fields2.downField("value").as[String] should beRight(s"@targeted-person-$commentId, #targeted-channel-$commentId")
        fields2.downField("short").as[Boolean] should beRight(false)
      }

      And("the slack additional channel should receive the publish post request with valid attachment")
      eventually {

        val newCommentRequests = slackApiMockServer.findPostRequestsFor("/api/chat.postMessage").drop(2)
        val doc: Json = parse(newCommentRequests.last.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        cursor.get[String]("channel") should beRight("CHANNEL_ID2")

        val message = cursor.downField("attachments").downArray.first

        message.get[String]("pretext") should beRight("New comment has been discovered")
        message.get[String]("color") should beRight("good")
        message.get[String]("author_name") should beRight("lachatak")
        message.get[String]("author_icon") should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.get[String]("title") should beRight(s"discussion-title-$discussionId1")
        message.get[String]("title_link") should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId")

        val field0 = message.downField("fields").downArray.first
        field0.get[String]("title") should beRight("Team")
        field0.get[String]("value") should beRight("Test Team")
        field0.get[Boolean]("short") should beRight(true)

        val field1 = message.downField("fields").downArray.first.right
        field1.get[String]("title") should beRight("Comments")
        field1.get[String]("value") should beRight("1")
        field1.get[Boolean]("short") should beRight(true)

        val fields2 = message.downField("fields").downArray.first.right.right
        fields2.downField("title").as[String] should beRight("Targeted")
        fields2.downField("value").as[String] should beRight(s"@targeted-person-$commentId, #targeted-channel-$commentId")
        fields2.downField("short").as[Boolean] should beRight(false)
      }
    }
  }

  feature("Internal status endpoint") {
    scenario("Status should return successfully") {
      When("Hitting the status endpoint")
      val uri = org.http4s.Uri.unsafeFromString(s"http://localhost:${ appHttpPort.value }").withPath("/internal/status")
      val response = BlazeClientBuilder[IO](ec).resource.use(_.expect[String](uri)).unsafeRunSync()

      Then("we should receive empty body")
      response shouldBe empty
    }
  }

  def resetTables(): Unit = {
    Logger[IO].warn("Reset tables before next test!").unsafeRunSync()
    (sql"TRUNCATE TABLE team CASCADE".update.run >> sql"TRUNCATE TABLE author CASCADE".update.run).transact(transactor).void.unsafeRunSync()
  }

  def findDiscussionAggregateRootPersistence(teamId: Long, discussionId: Long): Option[DiscussionAggregateRootPersistence] = {
    val program: OptionT[doobie.ConnectionIO, DiscussionAggregateRootPersistence] = for {
      team <- OptionT(findTeamQuery(teamId).option)
      discussion <- OptionT(findDiscussionQuery(teamId, discussionId).option)
      comments <- OptionT.liftF(findCommentQuery(teamId, discussionId).nel)
    } yield DiscussionAggregateRootPersistence(team, discussion, comments)

    program.value.transact(transactor).unsafeRunSync()
  }

  def findTeamQuery(teamId: Long): Query0[TeamPersistence] =
    sql"""
          SELECT id, name, description, created_at, updated_at
          FROM team
          WHERE id = $teamId
      """.query[TeamPersistence]

  def findDiscussionQuery(teamId: Long, discussionId: Long): Query0[DiscussionPersistence] =
    sql"""
          SELECT team_id, discussion_id, title, created_at, updated_at
          FROM discussion
          WHERE team_id = $teamId AND discussion_id = $discussionId
      """.query[DiscussionPersistence]

  def findCommentQuery(teamId: Long, discussionId: Long): Query0[CommentPersistence] =
    sql"""
          SELECT c.team_id, c.discussion_id, c.comment_id, a.id, a.name, a.avatar_url, c.url, c.body, c.body_version, c.created_at, c.updated_at
          FROM comment c
          JOIN author a ON a.id = c.author_id
          WHERE c.team_id = $teamId AND c.discussion_id = $discussionId
      """.query[CommentPersistence]

}


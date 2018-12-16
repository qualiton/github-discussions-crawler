package org.qualiton.crawler

import java.time.Instant
import java.util.concurrent.Executors

import scala.concurrent.duration._

import _root_.cats.syntax.flatMap._
import _root_.cats.syntax.functor._
import cats.effect.{ ContextShift, IO }
import cats.scalatest.{ EitherMatchers, ValidatedValues }
import fs2.concurrent.SignallingRef

import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import doobie.util.transactor.Transactor
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.{ Uri, Url }
import eu.timepit.refined.types.net.UserPortNumber
import io.circe.{ HCursor, Json }
import io.circe.parser.parse
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, PublisherConfig, SlackConfig }
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ CommentPersistence, CommentsListPersistence, DiscussionPersistence }
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
    with LazyLogging {

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
          maximumPoolSize = 5))

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
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName)
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()

      Then("new discussion is persisted to db")
      eventually {
        val result = findDiscussionBy(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionPersistence(teamId, teamName, discussionId, title, author, avatarUrl, body, _, discussionUrl, CommentsListPersistence(comments), _, _)) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            author should ===("lachatak")
            avatarUrl should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            body should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            discussionUrl should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            comments should have size 0L
        }
      }

      And("slack should receive the publish post request with valid attachment")
      eventually {
        val request = slackApiMockServer.findLastPostRequestFor("/api/chat.postMessage")

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
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
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName)
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()

      Then("new discussion is persisted to db")
      eventually {
        val result = findDiscussionBy(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionPersistence(
          teamId, teamName, discussionId, title, author, avatarUrl, body, _, discussionUrl,
          c@CommentsListPersistence(List(
          CommentPersistence(commentId2, commentAuthor2, commentAuthorAvatar2, commentBody2, _, commentUrl2, _, _),
          CommentPersistence(commentId1, commentAuthor1, commentAuthorAvatar1, commentBody1, _, commentUrl1, _, _))),
          _, _)) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            author should ===("lachatak")
            avatarUrl should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            body should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            discussionUrl should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            c.comments should have size 2L

            commentId2 should ===(commentId2)
            commentAuthor2 should ===("lachatak")
            commentAuthorAvatar2 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            commentBody2 should ===(s"comment-body-$commentId2 @targeted-person-$commentId2 #targeted-channel-$commentId2")
            commentUrl2 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId2")

            commentId1 should ===(commentId1)
            commentAuthor1 should ===("lachatak")
            commentAuthorAvatar1 should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            commentBody1 should ===(s"comment-body-$commentId1 @targeted-person-$commentId1 #targeted-channel-$commentId1")
            commentUrl1 should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId1")
        }
      }

      And("slack should receive the publish post request with valid attachment")
      eventually {
        val request = slackApiMockServer.findLastPostRequestFor("/api/chat.postMessage")

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        val message = cursor.downField("attachments").downArray.first

        message.get[String]("pretext") should beRight("New discussion has been discovered")
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
      slackApiMockServer.mockConversationsList(appConfig.publisherConfig.slackConfig.defaultChannelName)
      slackApiMockServer.mockRtmConnect()
      slackApiMockServer.mockChatPostMessage()
      val firstUpdatedAt: Instant = eventually {
        val result = findDiscussionBy(teamId1, discussionId1)
        result.value.updatedAt
      }

      Then("a new comment is discovered")
      githubApiV3MockServer.mockDiscussions(teamId1, 1, 1, referenceInstant)

      Then("the new comment is persisted to db")
      eventually {
        val result = findDiscussionBy(teamId1, discussionId1)

        inside(result) {
          case Some(DiscussionPersistence(
          teamId, teamName, discussionId, title, author, avatarUrl, body, _, discussionUrl,
          c@CommentsListPersistence(List(
          CommentPersistence(commentId, commentAuthor, commentAuthorAvatar, commentBody, _, commentUrl, _, commentLatestUpdatedAt))),
          _, latestUpdatedAt)) =>
            teamId should ===(teamId1)
            teamName should ===("Test Team")
            discussionId should ===(discussionId1)
            title should ===(s"discussion-title-$discussionId1")
            author should ===("lachatak")
            avatarUrl should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            body should ===(s"discussion-body-$discussionId1 @targeted-person-$discussionId1 #targeted-channel-$discussionId1")
            discussionUrl should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            c.comments should have size 1L
            latestUpdatedAt isAfter firstUpdatedAt
            commentLatestUpdatedAt isAfter firstUpdatedAt

            commentId should ===(commentId)
            commentAuthor should ===("lachatak")
            commentAuthorAvatar should ===("https://avatars0.githubusercontent.com/u/5830214?v=4")
            commentBody should ===(s"comment-body-$commentId @targeted-person-$commentId #targeted-channel-$commentId")
            commentUrl should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1/comments/$commentId")
        }
      }

      And("slack should receive the publish post request with valid attachment")
      eventually {
        val request = slackApiMockServer.findLastPostRequestFor("/api/chat.postMessage")

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
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
    logger.warn("Reset discussion table before next test!")
    sql"TRUNCATE TABLE discussion".update.run.transact(transactor).void.unsafeRunSync()
  }

  def findDiscussionBy(teamId: Long, discussionId: Long): Option[DiscussionPersistence] =
    sql"""
      SELECT team_id, team_name, discussion_id, title, author, avatar_url, body, body_version, discussion_url, comments, created_at, updated_at
      FROM discussion
      WHERE team_id = $teamId AND discussion_id = $discussionId""".query[DiscussionPersistence].option.transact(transactor).unsafeRunSync()
}


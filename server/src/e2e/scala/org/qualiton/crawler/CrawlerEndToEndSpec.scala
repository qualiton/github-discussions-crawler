package org.qualiton.crawler

import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import _root_.cats.syntax.flatMap._
import _root_.cats.syntax.functor._
import cats.effect.{ ExitCode, IO, IOApp }
import cats.scalatest.{ EitherMatchers, ValidatedValues }
import fs2.concurrent.SignallingRef

import doobie.implicits._
import doobie.util.transactor.Transactor
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.net.UserPortNumber
import io.circe.{ HCursor, Json }
import io.circe.parser.parse
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, SlackConfig }
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ CommentsListPersistence, DiscussionPersistence }
import org.qualiton.crawler.server.config.ServiceConfig
import org.qualiton.crawler.server.main.Server
import org.qualiton.crawler.testsupport.dockerkit.PostgresDockerTestKit
import org.qualiton.crawler.testsupport.scalatest.{ GithubApiV3MockServerSupport, SlackIncomingWebhookMockServerSupport }
import org.qualiton.crawler.testsupport.wiremock.{ GithubApiV3MockServer, SlackIncomingWebhookMockServer }

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
    with SlackIncomingWebhookMockServerSupport
    with PostgresDockerTestKit
    with Inside
    with IOApp {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(10, Seconds),
      interval = Span(100, Millis))

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetTables()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    run(List.empty).unsafeToFuture()
    ()
  }

  override def afterAll(): Unit = {
    (stopSignal.set(true) >> dataSource.close).unsafeRunSync()
    super.afterAll()
    ()
  }

  val appHttpPort: UserPortNumber = 9000

  val appConfig =
    ServiceConfig(
      httpPort = appHttpPort,
      gitConfig = GitConfig(
        baseUrl = refineV[Uri](s"http://localhost:${ GithubApiV3MockServer.GithubApiV3Port }").getOrElse(throw new IllegalArgumentException),
        requestTimeout = 5.seconds,
        apiToken = config.Secret(refineV[NonEmpty](GithubApiV3MockServer.testApiToken).getOrElse(throw new IllegalArgumentException)),
        refreshInterval = 1.second),
      slackConfig = SlackConfig(
        baseUri = refineV[Uri](s"http://localhost:${ SlackIncomingWebhookMockServer.SlackIncomingWebhookPort }").getOrElse(throw new IllegalArgumentException),
        requestTimeout = 5.seconds,
        apiToken = config.Secret(refineV[NonEmpty](SlackIncomingWebhookMockServer.testApiToken).getOrElse(throw new IllegalArgumentException)),
        enableNotificationPublish = true,
        ignoreEarlierThan = 6.hours),
      databaseConfig =
        DatabaseConfig(
          databaseDriverName = "org.postgresql.Driver",
          jdbcUrl = refineV[Uri](s"jdbc:postgresql://localhost:$postgresAdvertisedPort/postgres").getOrElse(throw new IllegalArgumentException),
          username = "postgres",
          password = config.Secret("postgres"),
          maximumPoolSize = 5))

  lazy val dataSource: DataSource[IO] = DataSource[IO](appConfig.databaseConfig, global, global).unsafeRunSync()

  lazy val transactor: Transactor[IO] = dataSource.hikariTransactor

  private val stopSignal: SignallingRef[IO, Boolean] = SignallingRef[IO, Boolean](false).unsafeRunSync()

  override def run(args: List[String]): IO[ExitCode] =
    Server.fromConfig(IO.pure(appConfig)).interruptWhen(stopSignal).compile.drain.map(_ => ExitCode.Success)

  feature("Crawler publishes new discussion discovered event") {
    scenario("New discussion is discovered with 0 comment and published to Slack") {
      When("When there is a new discussion with 0 comment")
      val teamId1 = 1L
      val discussionId1 = 1L
      val referenceInstant: Instant = Instant.now()
      mockGithubApiV3MockServer.mockDiscussions(teamId1, 1, 0, referenceInstant)

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
            body should ===(s"discussion-body-$discussionId1")
            discussionUrl should ===(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")
            comments should have size 0L
        }
      }

      And("slack should receive the publish post request with valid attachment")
      eventually {
        val request = mockSlackIncomingWebhookMockServer.incomingWebhookCallRequest

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        val message = cursor.downField("attachments").downArray.first

        message.downField("pretext").as[String] should beRight("New discussion has been discovered")
        message.downField("color").as[String] should beRight("good")
        message.downField("author_name").as[String] should beRight("lachatak")
        message.downField("author_icon").as[String] should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.downField("title").as[String] should beRight(s"discussion-title-$discussionId1")
        message.downField("title_link").as[String] should beRight(s"https://github.com/orgs/ovotech/teams/test-team/discussions/$discussionId1")

        val fields = message.downField("fields").downArray.first
        fields.downField("title").as[String] should beRight("Team")
        fields.downField("value").as[String] should beRight("Test Team")
        fields.downField("short").as[Boolean] should beRight(true)
      }
    }
  }

  feature("Internal status endpoint") {
    scenario("Status should return successfully") {
      When("Hitting the status endpoint")
      val uri = org.http4s.Uri.unsafeFromString(s"http://localhost:${ appHttpPort.value }").withPath("/internal/status")
      val response = BlazeClientBuilder[IO](global).resource.use(_.expect[String](uri)).unsafeRunSync()

      Then("we should receive empty body")
      response shouldBe empty
    }
  }

  def resetTables(): Unit =
    sql"TRUNCATE TABLE discussion".update.run.transact(transactor).void.unsafeRunSync()

  def findDiscussionBy(teamId: Long, discussionId: Long): Option[DiscussionPersistence] =
    sql"""
      SELECT team_id, team_name, discussion_id, title, author, avatar_url, body, body_version, discussion_url, comments, created_at, updated_at
      FROM discussion
      WHERE team_id = $teamId AND discussion_id = $discussionId""".query[DiscussionPersistence].option.transact(transactor).unsafeRunSync()
}

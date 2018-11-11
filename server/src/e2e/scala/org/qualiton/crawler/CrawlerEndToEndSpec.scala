package org.qualiton.crawler

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.{ ExitCode, IO, IOApp }
import cats.scalatest.{ EitherMatchers, ValidatedValues }
import fs2.concurrent.SignallingRef

import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.Uri
import io.circe.{ HCursor, Json }
import io.circe.parser._
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, SlackConfig }
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
    with GithubApiV3MockServerSupport
    with SlackIncomingWebhookMockServerSupport
    with PostgresDockerTestKit
    with Inside
    with IOApp {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(10, Seconds),
      interval = Span(100, Millis))

  override def beforeAll(): Unit = {
    super.beforeAll()
    run(List.empty).unsafeToFuture()
    ()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopSignal.set(true)
    ()
  }

  val appConfig =
    ServiceConfig(
      httpPort = 9000,
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

  private val stopSignal: SignallingRef[IO, Boolean] = SignallingRef[IO, Boolean](false).unsafeRunSync()

  override def run(args: List[String]): IO[ExitCode] = {
    Server.fromConfig(IO.pure(appConfig)).interruptWhen(stopSignal).compile.drain.map(_ => ExitCode.Success)
  }

  feature("Crawler publishes new discussion discovered event") {
    scenario("New discussion is discovered and published with 0 comment") {
      When("When there is a new discussion")
      mockGithubApiV3MockServer.mockDiscussions(1, 1, 0)

      Then("slack should receive the publish post call with valid attachment")
      mockSlackIncomingWebhookMockServer.mockIncomingWebhook()
      eventually {
        val request = mockSlackIncomingWebhookMockServer.incomingWebhookCallRequest

        val doc: Json = parse(request.getBodyAsString).getOrElse(Json.Null)
        val cursor: HCursor = doc.hcursor
        val message = cursor.downField("attachments").downArray.first

        message.downField("pretext").as[String] should beRight("New discussion has been discovered")
        message.downField("color").as[String] should beRight("good")
        message.downField("author_name").as[String] should beRight("lachatak")
        message.downField("author_icon").as[String] should beRight("https://avatars0.githubusercontent.com/u/5830214?v=4")
        message.downField("title").as[String] should beRight("discussion-title-1")
        message.downField("title_link").as[String] should beRight("https://github.com/orgs/ovotech/teams/test-team/discussions/1")

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
      val uri = org.http4s.Uri.unsafeFromString(s"http://localhost:9000").withPath("/internal/status")
      val response = BlazeClientBuilder[IO](global).resource.use(_.expect[String](uri)).unsafeRunSync()

      Then("we should receive empty body")
      response shouldBe empty
    }
  }
}

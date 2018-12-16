package org.qualiton.slack.rtm

import java.util.concurrent.Executors

import scala.concurrent.duration._

import cats.effect.{ ContextShift, IO, Timer }
import cats.scalatest.{ EitherMatchers, EitherValues }

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.refineV
import eu.timepit.refined.string.Url
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ BeforeAndAfterAll, FreeSpec, Inside, Matchers, OptionValues }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

import org.qualiton.slack.rtm.slack.models.SlackEvent
import org.qualiton.slack.testsupport.scalatest.{ SlackApiMockServerSupport, SlackRtmApiMockServerSupport }
import org.qualiton.slack.testsupport.wiremock.SlackApiMockServer.{ testApiToken, SlackApiHost, SlackApiPort }

class SlackRtmApiClientIntegrationSpec extends FreeSpec
  with Matchers
  with BeforeAndAfterAll
  with EitherValues
  with EitherMatchers
  with OptionValues
  with Eventually
  with TypeCheckedTripleEquals
  with Inside
  with SlackApiMockServerSupport
  with SlackRtmApiMockServerSupport
  with LazyLogging {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(15, Seconds),
      interval = Span(100, Millis))

  "SlackRtmApiClient" - {
    "stream" in {

      slackApiMockServer.mockRtmConnect()

      implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
      implicit val es = Executors.newCachedThreadPool()

      implicit def timer: Timer[IO] = IO.timer(ec)

      implicit val cs: ContextShift[IO] = IO.contextShift(ec)

      val pipe: fs2.Pipe[IO, SlackEvent, Unit] = _.evalMap(e => IO(logger.info(s"log: ${ e.toString }")))

      val program = for {
        slackRtmClient <- org.qualiton.slack.rtm.SlackRtmApiClient.stream[IO](
          token = testApiToken,
          pingInterval = 2.seconds,
          slackApiUrl = refineV[Url](s"http://$SlackApiHost:$SlackApiPort").getOrElse(throw new IllegalArgumentException))
        event <- slackRtmClient.events
      } yield event

      program
        .through(pipe)
        .take(2)
        .compile
        .drain.unsafeRunSync()
    }
  }

}

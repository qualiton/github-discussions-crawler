package org.qualiton.slack.testsupport.wiremock


import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{ ContextShift, IO, Timer }
import fs2.{ Pipe, Stream }
import fs2.concurrent.SignallingRef

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser._
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import scodec.Codec
import scodec.codecs._
import spinoco.fs2.http
import spinoco.fs2.http.websocket._

import org.qualiton.slack.rtm.slack.models.{ Hello, Pong, SlackEvent }
import org.qualiton.slack.rtm.SlackRtmApiClient.{ ClientMessage, Ping }
import org.qualiton.slack.testsupport.wiremock.SlackRtmApiMockServer.SlackRtmApiPort

class SlackRtmApiMockServer(slackRtmApiPort: Int = SlackRtmApiPort) extends Http4sDsl[IO] with LazyLogging {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val ES = Executors.newCachedThreadPool()
  implicit val ACG = AsynchronousChannelGroup.withThreadPool(ES)

  implicit val timer: Timer[IO] = IO.timer(ec)

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  implicit val codecString: Codec[String] = utf8

  implicit val snakeCaseConfig: Configuration =
    Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames

  private val stopSignal: SignallingRef[IO, Boolean] = SignallingRef[IO, Boolean](false).unsafeRunSync()

  private val wspipe: Pipe[IO, Frame[String], Frame[String]] = { in =>
    val clientStream: Stream[IO, SlackEvent] = in
      .through(decodeFrame)
      .collect {
        case Ping(id, _) => Pong(id)
      }

    (Stream.emit(Hello()) ++ clientStream)
      .map(e => Frame.Text(e.asJson.toString()))
  }

  private val decodeFrame: Pipe[IO, Frame[String], ClientMessage] =
    for {
      frame <- _
      decoded <- Stream.eval(IO(decode[ClientMessage](frame.a)))
      event <- decoded match {
        case Right(event) => Stream.emit(event)
        case Left(error) =>
          logger.warn(s"Event cannot be processed ($error): ${ frame.a }")
          Stream.empty
      }
    } yield event

  def startMockServer(): Unit =
    http
      .server[IO](new InetSocketAddress("127.0.0.1", slackRtmApiPort))(server(wspipe))
      .interruptWhen(stopSignal)
      .compile.drain.unsafeRunAsyncAndForget()

  def stopMockServer(): Unit =
    stopSignal.set(true).unsafeRunSync()

}

object SlackRtmApiMockServer {

  val SlackRtmApiHost: String = "localhost"
  val SlackRtmApiPort: Int = 3002

}







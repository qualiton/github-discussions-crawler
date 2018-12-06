package org.qualiton.slack.rtm


import java.net.URI
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ExecutorService

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Effect, Timer }
import fs2.{ Pipe, Stream }
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser._
import io.circe.syntax._
import org.http4s.client.blaze.BlazeClientBuilder
import scodec.Codec
import scodec.codecs._
import spinoco.fs2.http.websocket.{ Frame, WebSocket, WebSocketRequest }
import spinoco.protocol.http.HttpResponseHeader

import org.qualiton.slack.{ SlackApiClient, SlackApiHttp4sClient }
import org.qualiton.slack.rtm.SlackRtmClient.{ Message, Ping }
import org.qualiton.slack.rtm.slack.models.SlackEvent

class SlackRtmClient[F[_] : ConcurrentEffect] private(val slackApiClient: SlackApiClient[F])(implicit CS: ContextShift[F], T: Timer[F], AG: AsynchronousChannelGroup) extends LazyLogging {

  val F = implicitly[Effect[F]]

  implicit val snakeCaseConfig: Configuration =
    Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames

  private implicit val codecString: Codec[String] = utf8
  private val idCounter = new AtomicLong(1L)

  def events: Stream[F, SlackEvent] =
    for {
      toClientQueue <- Stream.eval(Queue.bounded[F, SlackEvent](100))
      ws = through(_.evalMap(toClientQueue.enqueue1) >> Stream.empty)
      toClient = toClientQueue.dequeue
      stream <- Stream(toClient, ws.drain).parJoin(2)
    } yield stream

  def through(clientPipe: Pipe[F, SlackEvent, Message]): Stream[F, Option[HttpResponseHeader]] = {
    val program = for {
      request <- wssRequest
      ws <- WebSocket.client[F, String, String](request, wspipe(clientPipe))
    } yield ws

    program.handleErrorWith(_ => through(clientPipe))
  }

  private def wssRequest: Stream[F, WebSocketRequest] =
    for {
      rtmConnect <- Stream.eval(slackApiClient.connectRealTimeMessageSession)
      url = new URI(rtmConnect.url)
      request <- Stream.emit(WebSocketRequest.wss(url.getHost, url.getPath)).covary
    } yield request

  private def wspipe(clientPipe: Pipe[F, SlackEvent, Message])(implicit T: Timer[F]): Pipe[F, Frame[String], Frame[String]] = { inbound =>
    val ping: Stream[F, Frame[String]] = Stream.awakeEvery[F](60.second).map(_ => Frame.Text(Ping(idCounter.getAndIncrement()).asJson.toString()))
    val outbound = inbound.through(decodeFrame).through(clientPipe).map(m => Frame.Text(m.asJson.toString()))
    Stream(ping, outbound).parJoin(2)
  }

  private val decodeFrame: Pipe[F, Frame[String], SlackEvent] =
    for {
      frame <- _
      decoded <- Stream.eval(F.delay(decode[SlackEvent](frame.a)))
      event <- decoded match {
        case Right(event) => Stream.emit(event)
        case Left(error) =>
          logger.warn(s"Event cannot be processed ($error): ${ frame.a }")
          Stream.empty
      }
    } yield event

}

object SlackRtmClient extends LazyLogging {

  def stream[F[_] : ConcurrentEffect](slackApiClient: SlackApiClient[F])
    (implicit CS: ContextShift[F], T: Timer[F], AG: AsynchronousChannelGroup): Stream[F, SlackRtmClient[F]] =
    Stream.eval(Effect[F].delay(new SlackRtmClient(slackApiClient)))

  def stream[F[_] : ConcurrentEffect](token: NonEmptyString)
    (implicit CS: ContextShift[F], T: Timer[F], ec: ExecutionContext, es: ExecutorService): Stream[F, SlackRtmClient[F]] = {
    implicit val ACG = AsynchronousChannelGroup.withThreadPool(es)
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(5.seconds).stream
      slackApiClient <- SlackApiHttp4sClient.stream(client, token)
      slackRtmClient <- stream(slackApiClient)
    } yield slackRtmClient
  }

  sealed trait Message
  case class SendMessage(channelId: String, text: String, ts_thread: Option[String] = None) extends Message
  case class BotEditMessage(channelId: String, ts: String, text: String, as_user: Boolean = true, `type`: String = "chat.update") extends Message
  case class TypingMessage(channelId: String) extends Message
  case class Ping(id: Long, `type`: String = "ping") extends Message
}

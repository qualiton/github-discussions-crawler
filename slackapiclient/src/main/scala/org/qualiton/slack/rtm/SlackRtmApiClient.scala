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
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
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
import org.qualiton.slack.rtm.SlackRtmApiClient.{ ClientMessage, Ping }
import org.qualiton.slack.rtm.slack.models.SlackEvent

class SlackRtmApiClient[F[_] : ConcurrentEffect : ContextShift : Timer] private(val slackApiClient: SlackApiClient[F], pingInterval: FiniteDuration)
  (implicit AG: AsynchronousChannelGroup) extends LazyLogging {

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

  def through(clientPipe: Pipe[F, SlackEvent, ClientMessage]): Stream[F, Option[HttpResponseHeader]] = {
    val program = for {
      request <- webSocketRequest
      ws <- WebSocket.client[F, String, String](request, wspipe(clientPipe))
    } yield ws

    program
      .handleErrorWith(t => Stream.eval(F.delay(s"Error in ws stream, restart connection: $t")) >> through(clientPipe))
  }

  private def webSocketRequest: Stream[F, WebSocketRequest] = {
    def resolveBy(uri: URI): F[WebSocketRequest] =
      F.delay {
        logger.info(s"Connecting to $uri")
        val ws: (String, Int, String) => WebSocketRequest = (host, port, path) => WebSocketRequest.ws(host, port, path)
        val wss: (String, Int, String) => WebSocketRequest = (host, port, path) => WebSocketRequest.wss(host, port, path)

        (uri.getScheme, uri.getPort) match {
          case ("ws", -1) => ws(uri.getHost, 80, uri.getPath)
          case ("ws", port) => ws(uri.getHost, port, uri.getPath)
          case ("wss", -1) => wss(uri.getHost, 443, uri.getPath)
          case ("wss", port) => wss(uri.getHost, port, uri.getPath)
          case (_, _) => throw new IllegalArgumentException(s"Cannot connect to $uri!")
        }
      }

    for {
      rtmConnect <- Stream.eval(slackApiClient.connectRealTimeMessageSession)
      uri = new URI(rtmConnect.url)
      request <- Stream.eval(resolveBy(uri))
    } yield request
  }

  private def wspipe(clientPipe: Pipe[F, SlackEvent, ClientMessage]): Pipe[F, Frame[String], Frame[String]] = { inbound =>
    val ping: Stream[F, Frame[String]] = Stream.awakeEvery[F](pingInterval).map(_ => Frame.Text(Ping(idCounter.getAndIncrement()).asJson.toString()))
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

object SlackRtmApiClient extends LazyLogging {

  def stream[F[_] : ConcurrentEffect : Timer : ContextShift](slackApiClient: SlackApiClient[F], pingInterval: FiniteDuration)
    (implicit AG: AsynchronousChannelGroup): Stream[F, SlackRtmApiClient[F]] =
    Stream.eval(Effect[F].delay(new SlackRtmApiClient(slackApiClient, pingInterval)))

  def stream[F[_] : ConcurrentEffect : ContextShift : Timer](
      token: NonEmptyString,
      requestTimeout: Duration = 5.second,
      pingInterval: FiniteDuration = 60.seconds,
      slackApiUrl: String Refined Url = SlackApiClient.defaultSlackApiUrl)
    (implicit ec: ExecutionContext, es: ExecutorService): Stream[F, SlackRtmApiClient[F]] = {
    implicit val ACG = AsynchronousChannelGroup.withThreadPool(es)
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(requestTimeout).stream
      slackApiClient <- SlackApiHttp4sClient.stream(client, token, slackApiUrl)
      slackRtmClient <- stream(slackApiClient, pingInterval)
    } yield slackRtmClient
  }

  sealed trait ClientMessage
  case class Message(channelId: String, text: String, ts_thread: Option[String] = None, `type`: String = "message") extends ClientMessage
  case class Typing(channelId: String, `type`: String = "typing") extends ClientMessage
  case class Ping(id: Long, `type`: String = "ping") extends ClientMessage
}

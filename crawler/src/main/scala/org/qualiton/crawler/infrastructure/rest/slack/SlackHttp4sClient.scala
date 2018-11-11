package org.qualiton.crawler.infrastructure.rest.slack

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, Effect, Sync }
import fs2.Stream

import com.typesafe.scalalogging.LazyLogging
import enumeratum.{ CirceEnum, Enum, EnumEntry }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.string.MatchesRegex
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{ Headers, Method, Request, Status, Uri }
import org.http4s.MediaType.application.json
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Accept
import shapeless.{ Witness => W }

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.domain.slack.SlackClient
import org.qualiton.crawler.infrastructure.rest.slack.IncomingWebhookMessageAssembler.fromDomain

class SlackHttp4sClient[F[_] : Effect] private(client: Client[F], slackConfig: SlackConfig) extends SlackClient[F] with Http4sClientDsl[F] with LazyLogging {

  import slackConfig._

  override def sendDiscussionEvent(event: Event): F[Status] = {
    val requestBody = fromDomain(event).asJson

    val request = Request[F](
      method = Method.POST,
      uri = apiToken.value.split("/").foldLeft(Uri.unsafeFromString(baseUri))(_ / _),
      headers = Headers(Accept(json))).withEntity(requestBody)

    logger.debug(s"sending to ${ request.uri } - $requestBody")
    client.status(request)
  }
}

object SlackHttp4sClient {
  def stream[F[_] : ConcurrentEffect](slackConfig: SlackConfig)(implicit ec: ExecutionContext): Stream[F, SlackClient[F]] =
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(slackConfig.requestTimeout).stream
      slackClient <- Stream.eval(Sync[F].delay(new SlackHttp4sClient[F](client, slackConfig)))
    } yield slackClient

  type ColorCode = String Refined (MatchesRegex[W.`"#[0-9](6)"`.T])

  sealed abstract class Color(val code: String) extends EnumEntry

  object Color extends Enum[Color] with CirceEnum[Color] {

    case object Good extends Color("good")

    case object Warning extends Color("warning")

    case object Danger extends Color("danger")

    case class Custom(codeCode: ColorCode) extends Color(codeCode)

    val values = findValues
  }

  final case class Field(
      title: String,
      value: String,
      short: Boolean)

  final case class Attachment(
      pretext: String,
      color: String,
      author_name: String,
      author_icon: String,
      title: String,
      title_link: String,
      fields: List[Field],
      ts: Long)

  final case class IncomingWebhookMessage(attachments: List[Attachment])
}


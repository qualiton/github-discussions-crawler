package org.qualiton.crawler.slack

import cats.effect.{Effect, Sync}
import eu.timepit.refined.auto.autoUnwrap
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.refined._
import org.http4s.MediaType.application.json
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Accept
import org.http4s.{Headers, Method, Request, Status, Uri}
import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.slack.SlackClient.IncomingWebhookMessage

class SlackHttp4sClient[F[_] : Effect] private(client: Client[F], slackConfig: SlackConfig) extends SlackClient[F] with Http4sClientDsl[F] {

  import slackConfig._

  override def sendIncomingWebhookMessage(message: IncomingWebhookMessage): F[Status] = {
    val request = Request[F](
      method = Method.POST,
      uri = Uri.unsafeFromString(baseUrl).withPath(apiToken.value),
      headers = Headers(Accept(json))).withEntity(message.asJson)

    client.status(request)
  }
}

object SlackHttp4sClient {
  def stream[F[_] : Effect](client: Client[F], slackConfig: SlackConfig): Stream[F, SlackClient[F]] =
    Stream.eval(Sync[F].delay(new SlackHttp4sClient[F](client, slackConfig)))
}


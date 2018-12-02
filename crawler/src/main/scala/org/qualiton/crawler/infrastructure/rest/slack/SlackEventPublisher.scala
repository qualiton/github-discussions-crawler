package org.qualiton.crawler
package infrastructure.rest.slack

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, Effect }
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.{ Event, EventPublisher }
import org.qualiton.crawler.infrastructure.rest.slack.ChatMessageAssembler.fromDomain
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher.SlackEventPublisherError
import org.qualiton.slack.{ SlackApiClient, SlackApiHttp4sClient }

class SlackEventPublisher[F[_] : Effect] private(slackApiClient: SlackApiClient[F], defaultChannelName: NonEmptyString)
  extends EventPublisher[F]
    with Http4sClientDsl[F]
    with LazyLogging {

  val F = implicitly[Effect[F]]

  override def publishDiscussionEvent(event: Event): F[Unit] = {

    slackApiClient.findChannelByName(defaultChannelName)
      .flatMap(_.fold[F[Unit]](F.raiseError(SlackEventPublisherError(s"Channel `$defaultChannelName` is not defined in Slack"))) { channelId =>
        for {
          message <- fromDomain(event).delay
          _ <- slackApiClient.postChatMessage(channelId.id, message)
        } yield ()
      })
  }
}

object SlackEventPublisher {

  def stream[F[_] : ConcurrentEffect](slackConfig: SlackConfig)(implicit ec: ExecutionContext): Stream[F, EventPublisher[F]] =
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(slackConfig.requestTimeout).stream
      slackApiClient <- SlackApiHttp4sClient(client, slackConfig.apiToken.value, slackConfig.baseUrl).delay[F].stream
      slackEventPublisher <- new SlackEventPublisher[F](slackApiClient, slackConfig.defaultChannelName).delay[F].stream
    } yield slackEventPublisher

  case class SlackEventPublisherError(message: String) extends Exception(message)

}


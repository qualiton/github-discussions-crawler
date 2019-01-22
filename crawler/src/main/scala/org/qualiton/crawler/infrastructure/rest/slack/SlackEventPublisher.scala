package org.qualiton.crawler
package infrastructure.rest.slack

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.{ ConcurrentEffect, Effect, Timer }
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import fs2.Stream

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import scalacache.Cache
import scalacache.caffeine.CaffeineCache
import scalacache.memoization._
import scalacache.CatsEffect.modes.async

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.{ DiscussionEvent, EventPublisher }
import org.qualiton.crawler.infrastructure.rest.slack.ChatMessageAssembler.fromDomain
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher.SlackEventPublisherError
import org.qualiton.slack.{ SlackApiClient, SlackApiHttp4sClient }
import org.qualiton.slack.models.Channel

class SlackEventPublisher[F[_] : Effect] private(slackApiClient: SlackApiClient[F], defaultChannelName: NonEmptyString)
  extends EventPublisher[F]
    with Http4sClientDsl[F]
    with LazyLogging {

  val F = implicitly[Effect[F]]

  private implicit val channelCache: Cache[Option[Channel]] = CaffeineCache[Option[Channel]]

  override def publishDiscussionEvent(event: DiscussionEvent): F[Unit] = {
    findChannelByName(defaultChannelName)
      .flatMap(_.fold[F[Unit]](F.raiseError(SlackEventPublisherError(s"Channel `$defaultChannelName` is not defined in Slack"))) { channel =>
        for {
          message <- fromDomain(event).delay
          channelIds <- event.targeted.teams.toList.traverse(findChannelByName(_)).map(channel.id :: _.flatten.map(_.id.drop(1)))
          _ <- channelIds.traverse(slackApiClient.postChatMessage(_, message))
        } yield ()
      })
  }

  private def findChannelByName(channelName: String): F[Option[Channel]] = memoizeF[F, Option[Channel]](Some(10.minutes)) {
    for {
      _ <- logger.info(s"Resolving channelName $channelName").delay
      resolvableChannelName = if (channelName.startsWith("#")) channelName.substring(1) else channelName
      channel <- slackApiClient.findChannelByName(resolvableChannelName)
    } yield channel
  }
}

object SlackEventPublisher {

  def stream[F[_] : ConcurrentEffect](slackApiClient: SlackApiClient[F], defaultChannelName: NonEmptyString): Stream[F, EventPublisher[F]] =
    new SlackEventPublisher[F](slackApiClient, defaultChannelName).delay.stream

  def stream[F[_] : ConcurrentEffect : Timer](slackConfig: SlackConfig)(implicit ec: ExecutionContext, retryPolicy: RetryPolicy[F]): Stream[F, EventPublisher[F]] =
    for {
      retryClient <- BlazeClientBuilder[F](ec).withRequestTimeout(slackConfig.requestTimeout).stream.map(Retry(retryPolicy)(_))
      slackApiClient <- SlackApiHttp4sClient.stream(retryClient, slackConfig.apiToken.value, slackConfig.baseUrl)
      slackApiPublisher <- stream(slackApiClient, slackConfig.defaultChannelName)
    } yield slackApiPublisher

  case class SlackEventPublisherError(message: String) extends Exception(message)

}


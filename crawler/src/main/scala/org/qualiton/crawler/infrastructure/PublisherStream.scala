package org.qualiton.crawler
package infrastructure

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import io.chrisdavenport.log4cats.Logger
import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.common.config.PublisherConfig
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher
import org.qualiton.slack.rtm.SlackRtmApiClient
import org.qualiton.slack.rtm.SlackRtmApiClient.ClientMessage
import org.qualiton.slack.rtm.slack.models.SlackEvent

object PublisherStream {

  def apply[F[_] : ConcurrentEffect : Timer : ContextShift : Logger](
      eventPublisherQueue: Queue[F, DiscussionEvent],
      publisherConfig: PublisherConfig)(implicit ec: ExecutionContext, es: ExecutorService, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    import publisherConfig.slackConfig._

    val loggerPipe: fs2.Pipe[F, SlackEvent, ClientMessage] = _.evalMap(e => Logger[F].debug(s"log: ${ e.toString }")) >> Stream.empty

    def isEventRecent(event: DiscussionEvent): Boolean =
      Instant.now().minus(publisherConfig.ignoreEarlierThan.toHours, ChronoUnit.HOURS) isBefore event.createdAt

    for {
      eventPublisher <- SlackEventPublisher.stream(publisherConfig.slackConfig)
      slackRtmClient <- SlackRtmApiClient.stream(token = apiToken.value, slackApiUrl = baseUrl)
      loggerStream = slackRtmClient.through(loggerPipe)
      eventStream = eventPublisherQueue.dequeue
      event <- Stream(eventStream, loggerStream.drain).parJoin(2)
      _ <-
        if (publisherConfig.enableNotificationPublish && isEventRecent(event)) {
          Stream.eval(eventPublisher.publishDiscussionEvent(event))
        } else {
          Stream.eval(Logger[F].warn(s"Not sending update since event ($event) was created more than 6 hours ago!")) >>
          Stream.empty
        }
    } yield ()
  }
}

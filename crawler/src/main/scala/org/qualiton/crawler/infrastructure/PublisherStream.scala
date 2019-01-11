package org.qualiton.crawler
package infrastructure

import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging
import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.common.config.PublisherConfig
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher

object PublisherStream extends LazyLogging {

  def apply[F[_] : ConcurrentEffect : Timer : ContextShift](
      eventPublisherQueue: Queue[F, DiscussionEvent],
      publisherConfig: PublisherConfig)(implicit ec: ExecutionContext, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    def isEventRecent(event: DiscussionEvent): Boolean =
      Instant.now().minus(publisherConfig.ignoreEarlierThan.toHours, ChronoUnit.HOURS) isBefore event.createdAt

    for {
      eventPublisher <- SlackEventPublisher.stream(publisherConfig.slackConfig)
      event <- eventPublisherQueue.dequeue
      _ <-
        if (publisherConfig.enableNotificationPublish && isEventRecent(event)) {
          Stream.eval(eventPublisher.publishDiscussionEvent(event))
        } else {
          Stream.empty
        }
    } yield ()
  }
}

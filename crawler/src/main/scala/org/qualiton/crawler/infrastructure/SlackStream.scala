package org.qualiton.crawler.infrastructure

import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher

object SlackStream {

  def apply[F[_] : ConcurrentEffect : Timer](
      eventPublisherQueue: Queue[F, DiscussionEvent],
      slackConfig: SlackConfig)(implicit ec: ExecutionContext, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    def isEventRecent(event: DiscussionEvent): Boolean =
      Instant.now().minus(slackConfig.ignoreEarlierThan.toHours, ChronoUnit.HOURS) isBefore event.createdAt

    for {
      eventPublisher <- SlackEventPublisher.stream(slackConfig)
      event <- eventPublisherQueue.dequeue
      _ <-
        if (slackConfig.enableNotificationPublish && isEventRecent(event)) {
          Stream.eval(eventPublisher.publishDiscussionEvent(event))
        } else {
          Stream.empty
        }
    } yield ()
  }
}

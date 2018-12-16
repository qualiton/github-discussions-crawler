package org.qualiton.crawler
package infrastructure

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging
import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.rest.slack.SlackEventPublisher
import org.qualiton.slack.rtm.SlackRtmApiClient
import org.qualiton.slack.rtm.SlackRtmApiClient.ClientMessage
import org.qualiton.slack.rtm.slack.models.SlackEvent

object SlackStream extends LazyLogging {

  def apply[F[_] : ConcurrentEffect : Timer : ContextShift](
      eventPublisherQueue: Queue[F, DiscussionEvent],
      slackConfig: SlackConfig)(implicit ec: ExecutionContext, es: ExecutorService, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    val loggerPipe: fs2.Pipe[F, SlackEvent, ClientMessage] = _.evalMap(e => logger.info(s"log: ${ e.toString }").delay) >> Stream.empty

    def isEventRecent(event: DiscussionEvent): Boolean =
      Instant.now().minus(slackConfig.ignoreEarlierThan.toHours, ChronoUnit.HOURS) isBefore event.createdAt

    for {
      eventPublisher <- SlackEventPublisher.stream(slackConfig)
      slackRtmClient <- SlackRtmApiClient.stream(token = slackConfig.apiToken.value, slackApiUrl = slackConfig.baseUrl)
      loggerStream = slackRtmClient.through(loggerPipe)
      eventStream = eventPublisherQueue.dequeue
      event <- Stream(eventStream, loggerStream.drain).parJoin(2)
      _ <-
        if (slackConfig.enableNotificationPublish && isEventRecent(event)) {
          Stream.eval(eventPublisher.publishDiscussionEvent(event))
        } else {
          Stream.empty
        }
    } yield ()
  }
}

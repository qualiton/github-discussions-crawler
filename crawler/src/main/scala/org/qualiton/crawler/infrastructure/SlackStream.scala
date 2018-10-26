package org.qualiton.crawler.infrastructure

import scala.concurrent.ExecutionContext

import cats.effect.ConcurrentEffect
import fs2.Stream
import fs2.concurrent.Queue

import org.qualiton.crawler.common.config.SlackConfig
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.infrastructure.http.slack.SlackHttp4sClient

object SlackStream {

  def apply[F[_] : ConcurrentEffect](
      eventQueue: Queue[F, Event],
      slackConfig: SlackConfig)
    (implicit ec: ExecutionContext): Stream[F, Unit] =
    for {
      slackClient <- SlackHttp4sClient.stream(slackConfig)
      event <- eventQueue.dequeue
      _ <-
        if (slackConfig.enableNotificationPublish) {
          Stream.eval(slackClient.sendDiscussionEvent(event))
        } else {
          Stream.empty
        }
    } yield ()
}

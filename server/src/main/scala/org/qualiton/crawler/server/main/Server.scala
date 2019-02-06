package org.qualiton.crawler
package server.main

import java.util.concurrent.ExecutorService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging
import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.flyway.FlywayUpdater
import org.qualiton.crawler.infrastructure.{ GithubStream, HealthcheckHttpServerStream, PublisherStream }
import org.qualiton.crawler.server.config.ServiceConfig

object Server extends LazyLogging {

  def fromConfig[F[_] : ConcurrentEffect : ContextShift : Timer](loadConfig: F[ServiceConfig])
    (implicit ec: ExecutionContext, es: ExecutorService): Stream[F, ExitCode] = {

    implicit val retryPolicy = RetryPolicy[F](RetryPolicy.exponentialBackoff(2.minutes, 10), RetryPolicy.defaultRetriable)

    for {
      config <- Stream.eval(loadConfig)
      dataSource <- DataSource.stream(config.databaseConfig, ec, ec)
      _ <- Stream.eval(FlywayUpdater(dataSource))
      discussionEventQueue <- Stream.eval(Queue.bounded[F, DiscussionEvent](100))
      gitStream = GithubStream(discussionEventQueue, dataSource, config.gitConfig)
      publisherStream = PublisherStream(discussionEventQueue, config.publisherConfig)
      httpStream = HealthcheckHttpServerStream(config.httpPort)
      stream <- Stream(httpStream, gitStream.drain, publisherStream.drain)
        .parJoin(3)
        .handleErrorWith(t => Stream.eval_(logger.error(t.getMessage, t).delay))
    } yield stream
  }
}

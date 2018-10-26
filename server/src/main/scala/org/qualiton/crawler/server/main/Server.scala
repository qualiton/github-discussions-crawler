package org.qualiton.crawler.server.main

import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Sync }
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import fs2.concurrent.Queue

import com.typesafe.scalalogging.LazyLogging
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.flyway.FlywayUpdater
import org.qualiton.crawler.infrastructure.{ GithubStream, SlackStream }
import org.qualiton.crawler.server.config.ServiceConfig

object Server extends LazyLogging {

  def fromConfig[F[_] : ConcurrentEffect : ContextShift](loadConfig: F[ServiceConfig]): Stream[F, ExitCode] = {

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val loggerErrorHandler: Throwable => F[Unit] = (t: Throwable) => Sync[F].delay(logger.error(t.getMessage, t))

    val init = for {
      config <- loadConfig
      dataSource <- DataSource(config.databaseConfig, ec, ec)
    } yield (config, dataSource)

    val appStream: (ServiceConfig, DataSource[F]) => Stream[F, ExitCode] = (serviceConfig, dataSource) => {
      val stream: Stream[F, Unit] = for {
        _ <- Stream.eval(FlywayUpdater(dataSource))
        eventQueue <- Stream.eval(Queue.bounded[F, Event](100))
        gitStream = GithubStream(eventQueue, dataSource, serviceConfig.gitConfig, loggerErrorHandler)
        slackStream = SlackStream(eventQueue, serviceConfig.slackConfig)
        stream <- Stream(gitStream, slackStream)
          .parJoin(2)
          .handleErrorWith(t => Stream.eval_(loggerErrorHandler(t)))
      } yield stream

      stream.drain.as(ExitCode.Success)
    }

    val release: DataSource[F] => F[Unit] = _.close

    Stream.bracket(init)(r => release(r._2))
      .flatMap(appStream.tupled)
  }
}

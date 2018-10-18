package org.qualiton.crawler.server.main

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import fs2.concurrent.Queue
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.flyway.FlywayUpdater
import org.qualiton.crawler.git.GithubRepository.Result
import org.qualiton.crawler.git.GithubStream
import org.qualiton.crawler.server.config.ServiceConfig
import org.qualiton.crawler.slack.SlackStream

object Server {

  def fromConfig[F[_] : ConcurrentEffect : ContextShift](loadConfig: F[ServiceConfig]): Stream[F, ExitCode] = {

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val init = for {
      config <- loadConfig
      dataSource <- DataSource(config.databaseConfig, ec, ec)
    } yield (config, dataSource)

    val appStream: (ServiceConfig, DataSource[F]) => Stream[F, ExitCode] = (serviceConfig, dataSource) => {
      val stream: Stream[F, Unit] = for {
        _ <- Stream.eval(FlywayUpdater(dataSource))
        queue <- Stream.eval(Queue.bounded[F, Result](100))
        gitStream = GithubStream(queue, dataSource, serviceConfig.gitConfig)
        slackStream = SlackStream(queue, serviceConfig.slackConfig)
        stream <- Stream(gitStream, slackStream).parJoin(2)
      } yield stream

      stream.drain.as(ExitCode.Success)
    }

    val release: DataSource[F] => F[Unit] = _.close

    Stream.bracket(init)(r => release(r._2))
      .flatMap(appStream.tupled)
  }
}

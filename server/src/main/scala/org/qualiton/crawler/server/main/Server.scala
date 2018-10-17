package org.qualiton.crawler.server.main

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.flyway.FlywayUpdater
import org.qualiton.crawler.git.GithubStream
import org.qualiton.crawler.server.config.ServiceConfig

object Server {

  def fromConfig[F[_] : ConcurrentEffect : ContextShift](loadConfig: F[ServiceConfig]): Stream[F, ExitCode] = {

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val init = for {
      config <- loadConfig
      dataSource <- DataSource(config.databaseConfig, ec, ec)
    } yield (config, dataSource)

    val appStream: (ServiceConfig, DataSource[F]) => Stream[F, ExitCode] = (serviceConfig, dataSource) => {
      Stream.eval(FlywayUpdater(dataSource)).drain ++
        GithubStream(dataSource, serviceConfig.gitConfig).drain ++
        Stream.emit(ExitCode.Success)
    }

    val release: DataSource[F] => F[Unit] = _.close

    Stream.bracket(init)(r => release(r._2))
      .flatMap(appStream.tupled)
  }
}

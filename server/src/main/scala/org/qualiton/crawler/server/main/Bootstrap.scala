package org.qualiton.crawler.server.main

import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import fs2.StreamApp.ExitCode
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.flyway.FlywayUpdater
import org.qualiton.crawler.git.GithubStream
import org.qualiton.crawler.server.config.ServiceConfig

object Bootstrap {

  def fromConfig[F[_] : Effect](loadConfig: F[ServiceConfig]): Stream[F, ExitCode] = {

    val init = for {
      config <- loadConfig
      dataSource <- DataSource(config.databaseConfig)
    } yield (config, dataSource)

    val appStream: (ServiceConfig, DataSource[F]) => Stream[F, ExitCode] = (serviceConfig, dataSource) => {
      Stream.eval(FlywayUpdater(dataSource)).drain ++
        GithubStream(serviceConfig.gitConfig).drain ++
        Stream.emit(ExitCode.Success)
    }

    val release: DataSource[F] => F[Unit] = _.close

    Stream.bracket(init)(appStream.tupled, r => release(r._2))
  }
}

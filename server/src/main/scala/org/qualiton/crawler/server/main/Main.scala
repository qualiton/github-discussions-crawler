package org.qualiton.crawler.server.main

import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits.toFunctorOps

import org.qualiton.crawler.server.config.DefaultConfigLoader

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Server.fromConfig(IO(DefaultConfigLoader.loadOrThrow())).compile.drain.as(ExitCode.Success)

}

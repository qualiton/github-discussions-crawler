package org.qualiton.crawler
package server.main

import cats.effect.{ ExitCode, IO, IOApp }

import org.qualiton.crawler.common.concurrent.CachedExecutionContext
import org.qualiton.crawler.server.config.DefaultConfigLoader

object Main extends IOApp {

  implicit val ec = CachedExecutionContext.default

  override def run(args: List[String]): IO[ExitCode] =
    Server.fromConfig(IO(DefaultConfigLoader.loadOrThrow())).compile.drain.as(ExitCode.Success)
}

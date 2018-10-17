package org.qualiton.crawler.server.main

import cats.effect.IO
import fs2.{Stream, StreamApp}
import org.qualiton.crawler.server.config.DefaultConfigLoader

object Main extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    Bootstrap.fromConfig(IO(DefaultConfigLoader.loadOrThrow()))

}

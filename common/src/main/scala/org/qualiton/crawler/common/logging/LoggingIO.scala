package org.qualiton.crawler.common.logging

import cats.effect.IO

import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.StructuredLogger

trait LoggingIO {
  protected implicit lazy val logger: StructuredLogger[IO] = Slf4jLogger.getLogger
}

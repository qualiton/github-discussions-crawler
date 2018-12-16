package org.qualiton.crawler.server.config

import cats.Show

import eu.timepit.refined.types.net.UserPortNumber

import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, PublisherConfig }

final case class ServiceConfig(
    httpPort: UserPortNumber,
    gitConfig: GitConfig,
    publisherConfig: PublisherConfig,
    databaseConfig: DatabaseConfig)

object ServiceConfig {
  implicit def showConfig: Show[ServiceConfig] = Show.fromToString
}

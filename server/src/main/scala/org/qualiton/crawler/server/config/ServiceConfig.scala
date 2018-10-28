package org.qualiton.crawler.server.config

import cats.Show

import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, SlackConfig }

final case class ServiceConfig(
    gitConfig: GitConfig,
    slackConfig: SlackConfig,
    databaseConfig: DatabaseConfig)

object ServiceConfig {
  implicit def showConfig: Show[ServiceConfig] = Show.fromToString
}

package org.qualiton.crawler.server.config

import ciris._
import ciris.generic._
import ciris.refined._
import ciris.syntax._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{DatabaseConfig, GitConfig, SlackConfig}

import scala.concurrent.duration._

trait ConfigLoader {
  final def loadOrThrow(): ServiceConfig =
    load().orThrow()

  protected def load(): Either[ConfigErrors, ServiceConfig]
}

object DefaultConfigLoader extends ConfigLoader {

  def load(): Either[ConfigErrors, ServiceConfig] = {

    loadConfig(
      env[NonEmptyString]("GIT_API_TOKEN"),
      env[NonEmptyString]("SLACK_TOKEN")
    ) { (gitApiToken, slackToken) =>
      ServiceConfig(
        gitConfig = GitConfig(
          baseUrl = "https://api.github.com",
          requestTimeout = 5.seconds,
          apiToken = config.Secret(gitApiToken)),
        slackConfig = SlackConfig(
          baseUrl = "https://hooks.slack.com/services/",
          requestTimeout = 5.seconds,
          apiToken = config.Secret(slackToken)),
        databaseConfig =
          DatabaseConfig(
            databaseDriverName = "org.postgresql.Driver",
            connectionString = "jdbc:postgresql://localhost:5431/postgres",
            username = "postgres",
            password = config.Secret("postgres"),
            maximumPoolSize = 5))
    }
  }
}

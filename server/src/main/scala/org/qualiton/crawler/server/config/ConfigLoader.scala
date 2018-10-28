package org.qualiton.crawler.server.config

import ciris._
import ciris.generic._
import ciris.refined._
import ciris.syntax._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url
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
      env[NonEmptyString]("GITHUB_API_TOKEN"),
      env[NonEmptyString]("SLACK_API_TOKEN"),
      env[Option[Boolean]]("SLACK_DISABLE_PUBLISH"),
      env[String Refined Url]("DATABASE_JDBC_URL"),
      env[NonEmptyString]("DATABASE_USERNAME"),
      env[NonEmptyString]("DATABASE_PASSWORD")
    ) { (githubApiToken, slackApiToken, slackDisablePublish, dbJdbcUrl, dbUsername, dbPassword) =>
      ServiceConfig(
        gitConfig = GitConfig(
          baseUrl = "https://api.github.com",
          requestTimeout = 5.seconds,
          apiToken = config.Secret(githubApiToken)),
        slackConfig = SlackConfig(
          baseUri = "https://hooks.slack.com/services/",
          requestTimeout = 5.seconds,
          apiToken = config.Secret(slackApiToken),
          enableNotificationPublish = slackDisablePublish.map(!_).getOrElse(true)),
        databaseConfig =
          DatabaseConfig(
            databaseDriverName = "org.postgresql.Driver",
            jdbcUrl = dbJdbcUrl,
            username = dbUsername,
            password = config.Secret(dbPassword),
            maximumPoolSize = 3))
    }
  }
}

package org.qualiton.crawler.server.config

import scala.concurrent.duration._

import ciris._
import ciris.generic._
import ciris.refined._
import ciris.syntax._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.common.config
import org.qualiton.crawler.common.config.{ DatabaseConfig, GitConfig, SlackConfig }

trait ConfigLoader {
  final def loadOrThrow(): ServiceConfig =
    load().orThrow()

  protected def load(): Either[ConfigErrors, ServiceConfig]
}

object DefaultConfigLoader extends ConfigLoader {

  def load(): Either[ConfigErrors, ServiceConfig] = {

    loadConfig(
      env[NonEmptyString]("GITHUB_API_TOKEN"),
      env[Option[FiniteDuration]]("GITHUB_REFRESH_INTERVAL"),
      env[NonEmptyString]("SLACK_API_TOKEN"),
      env[Option[Boolean]]("SLACK_DISABLE_PUBLISH"),
      env[String Refined Uri]("DATABASE_JDBC_URL"),
      env[NonEmptyString]("DATABASE_USERNAME"),
      env[NonEmptyString]("DATABASE_PASSWORD"),
      env[Option[UserPortNumber]]("HTTP_PORT"),
    ) { (githubApiToken, githubRefreshInterval, slackApiToken, slackDisablePublish, dbJdbcUrl, dbUsername, dbPassword, httpPort) =>
      ServiceConfig(
        httpPort = httpPort.getOrElse(9000),
        gitConfig = GitConfig(
          baseUrl = "https://api.github.com",
          requestTimeout = 5.seconds,
          apiToken = config.Secret(githubApiToken),
          refreshInterval = githubRefreshInterval.getOrElse(1.minute)),
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

package org.qualiton.crawler
package common.datasource

import scala.concurrent.ExecutionContext

import cats.effect.{ ContextShift, Effect }
import fs2.Stream

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto.autoUnwrap

import org.qualiton.crawler.common.config.DatabaseConfig

object DataSource {

  def apply[F[_] : Effect : ContextShift](
      databaseConfig: DatabaseConfig,
      connectEC: ExecutionContext,
      transactEC: ExecutionContext): DataSource[F] =
    new DataSource[F](databaseConfig, connectEC, transactEC)

  def stream[F[_] : Effect : ContextShift](
      databaseConfig: DatabaseConfig,
      connectEC: ExecutionContext,
      transactEC: ExecutionContext): Stream[F, DataSource[F]] =
    Stream.bracket(DataSource[F](databaseConfig, connectEC, transactEC).delay)(_.close)
}

final class DataSource[F[_] : Effect : ContextShift] private(
    databaseConfig: DatabaseConfig,
    connectEC: ExecutionContext,
    transactEC: ExecutionContext) {

  import databaseConfig._

  lazy val hikariDataSource: HikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(databaseDriverName)
    hikariConfig.setJdbcUrl(jdbcUrl)
    hikariConfig.setUsername(username)
    hikariConfig.setPassword(password.value)
    hikariConfig.setMaximumPoolSize(maximumPoolSize)
    new HikariDataSource(hikariConfig)
  }

  def hikariTransactor = HikariTransactor(hikariDataSource, connectEC, transactEC)

  def close: F[Unit] =
    hikariDataSource.close().delay
}

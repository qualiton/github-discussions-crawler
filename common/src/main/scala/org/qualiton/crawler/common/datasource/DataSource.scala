package org.qualiton.crawler.common.datasource

import cats.effect.{Effect, Sync}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import eu.timepit.refined.auto.autoUnwrap
import org.qualiton.crawler.common.config.DatabaseConfig
import org.qualiton.crawler.common.util.Closeable

object DataSource {
  def apply[F[_] : Effect](databaseConfig: DatabaseConfig): F[DataSource[F]] = Sync[F].delay {
    new DataSource[F](databaseConfig)
  }
}

final class DataSource[F[_] : Effect] private(databaseConfig: DatabaseConfig) extends Closeable[F] {

  import databaseConfig._

  lazy val hikariDataSource: HikariDataSource = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(databaseDriverName)
    hikariConfig.setJdbcUrl(connectionString)
    hikariConfig.setUsername(username)
    hikariConfig.setPassword(password.value)
    hikariConfig.setMaximumPoolSize(maximumPoolSize)
    new HikariDataSource(hikariConfig)
  }

  def hikariTransactor = HikariTransactor(hikariDataSource)

  override def close: F[Unit] =
    Sync[F].delay {
      hikariDataSource.close()
    }
}

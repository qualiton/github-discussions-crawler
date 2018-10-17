package org.qualiton.crawler.flyway

import cats.effect.Sync
import org.flywaydb.core.Flyway
import org.qualiton.crawler.common.datasource.DataSource

object FlywayUpdater {

  def apply[F[_] : Sync](dataSource: DataSource[F]): F[Unit] =
    Sync[F].delay {
      val flyway: Flyway = new Flyway
      flyway.setDataSource(dataSource.hikariDataSource)
      flyway.migrate

      ()
    }
}

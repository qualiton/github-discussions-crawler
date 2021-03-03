package org.qualiton.crawler
package common.kamon

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import cats.effect.{ Resource, Sync }
import cats.syntax.flatMap._
import cats.syntax.functor._

import io.chrisdavenport.log4cats.Logger
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.system.SystemMetrics

import org.qualiton.crawler.common.config.KamonConfig

object KamonMetrics {

  def resource[F[_] : Sync : Logger, A](kamonConfig: KamonConfig): Resource[F, Unit] =
    Resource.make[F, Unit](start(kamonConfig))(_ => stop())

  private def start[F[_] : Sync : Logger](kamonConfig: KamonConfig): F[Unit] =
    for {
      _ <- Kamon.reconfigure(kamonConfig.asTypesafeConfig).delay
      _ <- SystemMetrics.startCollecting().delay
      _ <- Kamon.addReporter(new PrometheusReporter()).delay
      _ <- Logger[F].info("Kamon started!")
    } yield ()

  private def stop[F[_] : Sync : Logger](): F[Unit] =
    SystemMetrics.stopCollecting().delay >>
    Await.ready(Kamon.stopAllReporters(), 60 seconds).delay >>
    Logger[F].info("Kamon reporters stopped!")
}


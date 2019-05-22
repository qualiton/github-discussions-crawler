package org.qualiton.crawler.common.config

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

import com.typesafe.config.{ Config => TypesafeConfig, ConfigFactory => TypesafeConfigFactory }
import eu.timepit.refined.types.string.NonEmptyString

final case class KamonConfig(
    applicationName: NonEmptyString,
    tickInterval: FiniteDuration = 60.seconds) {

  def asTypesafeConfig: TypesafeConfig = TypesafeConfigFactory.parseMap((
    Map(
      "kamon.metric.tick-interval" -> tickInterval.toString,
      "kamon.environment.service" -> applicationName.value)
    ).asJava).withFallback(TypesafeConfigFactory.defaultReference)
}

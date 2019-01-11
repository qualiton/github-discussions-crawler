package org.qualiton.crawler.common.config

import scala.concurrent.duration.{ Duration, FiniteDuration }

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString

final case class GitConfig(
    baseUrl: String Refined Url,
    requestTimeout: Duration,
    apiToken: Secret[NonEmptyString],
    refreshInterval: FiniteDuration)

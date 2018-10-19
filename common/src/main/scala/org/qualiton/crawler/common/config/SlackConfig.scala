package org.qualiton.crawler.common.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.Duration

final case class SlackConfig(baseUrl: String Refined Uri,
                             requestTimeout: Duration,
                             apiToken: Secret[NonEmptyString],
                             enableNotificationPublish: Boolean)

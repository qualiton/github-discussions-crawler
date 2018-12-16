package org.qualiton.crawler.common.config

import scala.concurrent.duration.Duration

final case class PublisherConfig(
    slackConfig: SlackConfig,
    enableNotificationPublish: Boolean,
    ignoreEarlierThan: Duration)

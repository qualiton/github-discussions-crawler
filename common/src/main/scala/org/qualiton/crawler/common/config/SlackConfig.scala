package org.qualiton.crawler.common.config

import eu.timepit.refined.types.string.NonEmptyString

final case class SlackConfig(apiToken: Secret[NonEmptyString])

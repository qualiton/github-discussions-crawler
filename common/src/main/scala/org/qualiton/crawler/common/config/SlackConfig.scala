package org.qualiton.crawler.common.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import org.qualiton.crawler.common.config.SlackConfig.SlackChannelName
import shapeless.{Witness => W}

final case class SlackConfig(defaultChannel: SlackChannelName,
                             apiToken: Secret[NonEmptyString])

object SlackConfig {
  type SlackChannelName = String Refined (MatchesRegex[W.`"#[a-z_\\\\-]+"`.T])
}

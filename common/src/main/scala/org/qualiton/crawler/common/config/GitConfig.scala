package org.qualiton.crawler.common.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.string.NonEmptyString

final case class GitConfig(baseUrl: String Refined Uri,
                           apiToken: Secret[NonEmptyString])

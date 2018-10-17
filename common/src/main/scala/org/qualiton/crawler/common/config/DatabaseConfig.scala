package org.qualiton.crawler.common.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

final case class DatabaseConfig(databaseDriverName: NonEmptyString,
                                connectionString: String Refined Uri,
                                username: NonEmptyString,
                                password: Secret[NonEmptyString],
                                maximumPoolSize: PosInt)

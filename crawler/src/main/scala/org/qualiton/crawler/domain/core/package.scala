package org.qualiton.crawler.domain

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Url}
import eu.timepit.refined.types.string.NonEmptyString
import shapeless.{Witness => W}

package object core {

  type Addressee = String Refined (MatchesRegex[W.`"@[a-z]+"`.T])

}

package core {

  sealed trait Event

  final case class NewDiscussionCreatedEvent(author: NonEmptyString,
                                             title: NonEmptyString,
                                             url: String Refined Url,
                                             teamName: NonEmptyString,
                                             addressee: List[Addressee],
                                             createdAt: Instant) extends Event

  final case class NewCommentAddedEvent(author: NonEmptyString,
                                        title: NonEmptyString,
                                        url: String Refined Url,
                                        teamName: NonEmptyString,
                                        numberOfComments: Int,
                                        addressee: List[Addressee],
                                        createdAt: Instant) extends Event

}

package org.qualiton.crawler.domain

import java.time.Instant

import cats.data.NonEmptyList

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ MatchesRegex, Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString
import shapeless.{ Witness => W }

package object core {

  type AddresseeSpec = MatchesRegex[W.`"@[a-z]+"`.T]
  type Addressee = String Refined AddresseeSpec

  type Url = String Refined RefinedUrl
}

package core {

  sealed trait Event {
    def createdAt: Instant
  }

  final case class NewDiscussionDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      author: NonEmptyString,
      avatarUrl: Url,
      discussionUrl: Url,
      totalCommentsCount: Int,
      addressees: Set[Addressee],
      override val createdAt: Instant) extends Event

  final case class NewCommentsDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      totalCommentsCount: Int,
      newComments: NonEmptyList[NewComment],
      override val createdAt: Instant) extends Event

  final case class NewComment(
      author: NonEmptyString,
      avatarUrl: Url,
      commentUrl: Url,
      addressees: Set[Addressee],
      createdAt: Instant)

}

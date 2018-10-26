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

  sealed trait Event

  final case class NewDiscussionDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      author: NonEmptyString,
      url: Url,
      totalCommentsCount: Int,
      addressees: Set[Addressee],
      createdAt: Instant) extends Event

  final case class NewCommentsDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      totalCommentsCount: Int,
      comments: NonEmptyList[NewComment]) extends Event

  final case class NewComment(
      author: NonEmptyString,
      url: Url,
      addressees: Set[Addressee],
      createdAt: Instant)

}

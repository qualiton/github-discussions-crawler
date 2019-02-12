package org.qualiton.crawler.domain

import java.time.Instant

import cats.data.NonEmptyList

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ MatchesRegex, Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString
import shapeless.{ Witness => W }

package object core {

  type TargetedPersonSpec = MatchesRegex[W.`"\\\\@[0-9a-zA-Z_\\\\-]+"`.T]
  type TargetedPerson = String Refined TargetedPersonSpec

  type TargetedTeamSpec = MatchesRegex[W.`"\\\\#[0-9a-zA-Z_\\\\-]+"`.T]
  type TargetedTeam = String Refined TargetedTeamSpec

  type Url = String Refined RefinedUrl
}

package core {

  sealed trait DiscussionEvent {
    def createdAt: Instant

    def targeted: Targeted
  }

  final case class NewDiscussionDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      author: NonEmptyString,
      avatarUrl: Url,
      discussionUrl: Url,
      totalCommentsCount: Int,
      targeted: Targeted,
      override val createdAt: Instant) extends DiscussionEvent

  final case class NewCommentsDiscoveredEvent(
      teamName: NonEmptyString,
      title: NonEmptyString,
      totalCommentsCount: Int,
      newComments: NonEmptyList[NewComment],
      override val createdAt: Instant) extends DiscussionEvent {

    val targeted: Targeted =
      Targeted(
        persons = newComments.foldLeft(Set.empty[TargetedPerson])(_ ++ _.targeted.persons),
        teams = newComments.foldLeft(Set.empty[TargetedTeam])(_ ++ _.targeted.teams))
  }

  final case class NewComment(
      author: NonEmptyString,
      avatarUrl: Url,
      commentUrl: Url,
      targeted: Targeted,
      updatedAt: Instant)

  final case class Targeted(
      persons: Set[TargetedPerson],
      teams: Set[TargetedTeam]) {

    val isEmpty: Boolean = persons.isEmpty && teams.isEmpty
  }

}

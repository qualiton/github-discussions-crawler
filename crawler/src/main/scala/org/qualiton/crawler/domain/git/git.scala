package org.qualiton.crawler.domain

import java.time.Instant

import cats.data.NonEmptyList

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import shapeless.{ Witness => W }

import org.qualiton.crawler.domain.core.Url

package object git {
  type TeamDiscussionIdSpec = MatchesRegex[W.`"[0-9]*_[0-9]*"`.T]
  type TeamDiscussionId = String Refined TeamDiscussionIdSpec

  type Id = Long Refined NonNegative
}

package git {

  sealed trait Targetable {
    private val Person = """(@[0-9a-zA-Z_\-]+)""".r
    private val Team = """(#[0-9a-zA-Z_\-]+)""".r

    def body: NonEmptyString

    val targetedPerson: Set[String] = Person.findAllMatchIn(body).map(_.group(1)).toSet
    val targetedTeam: Set[String] = Team.findAllMatchIn(body).map(_.group(1)).toSet
  }

  final case class TeamDiscussionAggregateRoot(
      team: Team,
      discussion: Discussion) {

    val id: TeamDiscussionId = refineV[TeamDiscussionIdSpec](s"${ team.id }_${ discussion.id }")
      .getOrElse(throw new IllegalArgumentException("Illegal key! Must match pattern `[0-9]*_[0-9]*`"))

    val initialComment = discussion.comments.head
    val totalCommentsCount = discussion.comments.size - 1
    val lastUpdated: Instant = (discussion.updatedAt :: discussion.comments.map(_.updatedAt).toList).max
  }

  final case class Discussion(
      id: Id,
      title: NonEmptyString,
      comments: NonEmptyList[Comment],
      createdAt: Instant,
      updatedAt: Instant)

  final case class Team(
      id: Id,
      name: NonEmptyString,
      description: String,
      createdAt: Instant,
      updatedAt: Instant)

  final case class Author(
      id: Id,
      name: NonEmptyString,
      avatarUrl: Url)

  final case class Comment(
      id: Id,
      url: Url,
      author: Author,
      body: NonEmptyString,
      bodyVersion: NonEmptyString,
      createdAt: Instant,
      updatedAt: Instant) extends Targetable

  final case class ValidationError(message: String, errors: List[Throwable] = List.empty) extends IllegalStateException(message) {
    def this(message: String) = this(message, List.empty)
  }

}

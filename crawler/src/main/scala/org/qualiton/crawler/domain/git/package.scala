package org.qualiton.crawler.domain

import java.time.Instant

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.domain.core.Url

package git {

  sealed trait Targetable {
    private val Person = """(@[0-9a-zA-Z_\\-]+)""".r
    private val Team = """(#[0-9a-zA-Z_\\-]+)""".r

    def body: NonEmptyString

    val targeted: Set[String] = Person.findAllMatchIn(body).map(_.group(1)).toSet ++ Team.findAllMatchIn(body).map(_.group(1)).toSet
  }

  final case class Discussion(
      teamId: Long,
      teamName: NonEmptyString,
      discussionId: Long,
      title: NonEmptyString,
      author: NonEmptyString,
      avatarUrl: Url,
      body: NonEmptyString,
      bodyVersion: NonEmptyString,
      discussionUrl: Url,
      comments: List[Comment],
      createdAt: Instant,
      updatedAt: Instant) extends Targetable

  final case class Comment(
      commentId: Long,
      author: NonEmptyString,
      avatarUrl: Url,
      body: NonEmptyString,
      bodyVersion: NonEmptyString,
      commentUrl: Url,
      createdAt: Instant,
      updatedAt: Instant) extends Targetable

  final case class ValidationError(message: String, errors: List[Throwable] = List.empty) extends IllegalStateException(message) {
    def this(message: String) = this(message, List.empty)
  }

}

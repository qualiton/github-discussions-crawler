package org.qualiton.crawler.domain

import java.time.Instant

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.string.NonEmptyString
import org.qualiton.crawler.domain.core.Url

package git {

  final case class Team(id: Long,
      name: NonEmptyString)

  sealed trait Targetable {
    private val Addressee = "(@[0-9a-zA-Z]+)".r

    def body: NonEmptyString

    val addressees: Set[String] = Addressee.findAllMatchIn(body).map(_.group(1)).toSet
  }

  final case class Discussion(id: Long,
                              title: NonEmptyString,
                              author: NonEmptyString,
                              body: NonEmptyString,
                              bodyVersion: NonEmptyString,
                              url: Url,
                              commentsCount: Long,
                              createdAt: Instant,
                              updatedAt: Instant) extends Targetable

  final case class Comment(id: Long,
                           author: NonEmptyString,
                           body: NonEmptyString,
                           bodyVersion: NonEmptyString,
                           url: Url,
                           createdAt: Instant) extends Targetable

  final case class TeamDiscussionDetails(team: Team,
                                         discussion: Discussion,
                                         comments: List[Comment])

  case class ValidationError(message: String) extends IllegalStateException(message)

}

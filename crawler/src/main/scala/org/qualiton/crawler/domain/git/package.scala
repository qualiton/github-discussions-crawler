package org.qualiton.crawler.domain

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString

package git {

  final case class Team(id: Long,
                        name: NonEmptyString,
                        createdAt: Instant,
                        updatedAt: Instant)

  final case class Discussion(id: Long,
                              title: NonEmptyString,
                              author: NonEmptyString,
                              body: NonEmptyString,
                              bodyVersion: NonEmptyString,
                              url: String Refined Url,
                              commentsCount: Long,
                              createdAt: Instant,
                              updatedAt: Instant)

  final case class Comment(id: Long,
                           author: NonEmptyString,
                           body: NonEmptyString,
                           bodyVersion: NonEmptyString,
                           url: String Refined Url,
                           createdAt: Instant)

  final case class TeamDiscussionDetails(team: Team,
                                         discussion: Discussion,
                                         comments: List[Comment])


}

package org.qualiton.crawler.domain.git

import java.time.Instant

trait GithubRepository[F[_]] {

  def findLastUpdatedAt: F[Instant]

  def find(id: TeamDiscussionId): F[Option[TeamDiscussionAggregateRoot]]

  def save(discussion: TeamDiscussionAggregateRoot): F[Unit]
}

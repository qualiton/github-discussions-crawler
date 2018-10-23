package org.qualiton.crawler.domain.git

import java.time.Instant

trait GithubRepository[F[_]] {

  def findLastUpdatedAt: F[Instant]

  def find(teamId: Long, discussionId: Long): F[Either[Throwable, Option[TeamDiscussionDetails]]]

  def save(teamDiscussionDetails: TeamDiscussionDetails): F[Either[Throwable, Unit]]
}

package org.qualiton.crawler.domain.git

import java.time.Instant

import fs2.Stream

trait GithubClient[F[_]] {

  def getTeamDiscussionsUpdatedAfter(instant: Instant): Stream[F, Discussion]
}

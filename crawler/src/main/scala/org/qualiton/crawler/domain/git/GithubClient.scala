package org.qualiton.crawler.domain.git

import java.time.Instant

import cats.data.EitherT
import fs2.Stream

trait GithubClient[F[_]] {

  def getTeamDiscussionsUpdatedAfter(instant: Instant): EitherT[Stream[F, ?], Throwable, Discussion]
}

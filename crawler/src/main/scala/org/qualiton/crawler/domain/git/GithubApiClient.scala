package org.qualiton.crawler.domain.git

import java.time.Instant

import fs2.Stream

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url

trait GithubApiClient[F[_]] {

  def getTeamDiscussionsUpdatedAfter(instant: Instant): Stream[F, Discussion]
}

object GithubApiClient {

  val defaultGithubApiUrl: String Refined Url = "https://api.github.com"
}

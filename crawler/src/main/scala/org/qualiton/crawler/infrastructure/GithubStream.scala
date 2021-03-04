package org.qualiton.crawler
package infrastructure

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import io.chrisdavenport.log4cats.Logger
import org.http4s.client.middleware.RetryPolicy
import upperbound.Limiter

import org.qualiton.crawler.application.GithubDiscussionHandler
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.common.fs2.Supervision
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient

object GithubStream {

  def apply[F[_] : ConcurrentEffect : ContextShift : Timer : Logger](
      eventQueue: Queue[F, DiscussionEvent],
      dataSource: DataSource[F],
      gitConfig: GitConfig,
      githubApiLimiter: Limiter[F])
    (implicit ec: ExecutionContext, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    val program: Stream[F, Unit] = for {
      githubClient <- GithubHttp4sApiClient.stream(gitConfig, githubApiLimiter)
      repository <- GithubPostgresRepository.stream(dataSource)
      handler <- GithubDiscussionHandler.stream(eventQueue, githubClient, repository)
      _ <- Stream.awakeEvery[F](gitConfig.refreshInterval)
      _ <- handler.synchronizeDiscussions()
    } yield ()

    program.through(Supervision.logAndRestartOnError("github"))
  }
}

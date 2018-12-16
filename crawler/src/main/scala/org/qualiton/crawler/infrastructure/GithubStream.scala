package org.qualiton.crawler.infrastructure

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import org.http4s.client.middleware.RetryPolicy

import org.qualiton.crawler.application.GithubDiscussionHandler
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.core.DiscussionEvent
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient

object GithubStream {

  def apply[F[_] : ConcurrentEffect : ContextShift : Timer](
      eventQueue: Queue[F, DiscussionEvent],
      dataSource: DataSource[F],
      gitConfig: GitConfig,
      loggerErrorHandler: Throwable => F[Unit])
    (implicit ec: ExecutionContext, retryPolicy: RetryPolicy[F]): Stream[F, Unit] = {

    val program: Stream[F, Unit] = for {
      githubClient <- GithubHttp4sApiClient.stream(gitConfig)
      repository <- GithubPostgresRepository.stream(dataSource)
      handler <- GithubDiscussionHandler.stream(eventQueue, githubClient, repository)
      _ <- Stream.awakeEvery[F](gitConfig.refreshInterval)
      result <- handler.synchronizeDiscussions()
    } yield result

    program.attempt.flatMap {
      case Left(e) => Stream.eval_(loggerErrorHandler(e))
      case Right(value) => Stream.emit(value)
    }
  }
}

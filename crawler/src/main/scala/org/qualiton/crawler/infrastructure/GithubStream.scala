package org.qualiton.crawler.infrastructure

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.concurrent.Queue

import org.qualiton.crawler.application.GithubDiscussionHandler
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.core.Event
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sClient

object GithubStream {

  def apply[F[_] : ConcurrentEffect : ContextShift : Timer](
      eventQueue: Queue[F, Event],
      dataSource: DataSource[F],
      gitConfig: GitConfig,
      loggerErrorHandler: Throwable => F[Unit])
    (implicit ec: ExecutionContext): Stream[F, Unit] = {

    val program: Stream[F, Either[Throwable, Unit]] = for {
      githubClient <- GithubHttp4sClient.stream(gitConfig)
      repository <- GithubPostgresRepository.stream(dataSource)
      handler <- GithubDiscussionHandler.stream(eventQueue, githubClient, repository)
      _ <- Stream.awakeEvery[F](gitConfig.refreshInterval)
      result <- handler.synchronizeDiscussions()
    } yield result

    program.flatMap {
      case Left(e) => Stream.eval_(loggerErrorHandler(e))
      case Right(value) => Stream.emit(value)
    }
  }
}

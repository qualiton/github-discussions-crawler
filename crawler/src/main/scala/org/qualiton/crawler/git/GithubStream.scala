package org.qualiton.crawler.git

import cats.effect.{ConcurrentEffect, ContextShift}
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.client.blaze.BlazeClientBuilder
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.git.GithubRepository.{Result, TeamDiscussionCommentDetails}

import scala.concurrent.ExecutionContext

object GithubStream {

  def apply[F[_] : ConcurrentEffect : ContextShift](queue: Queue[F, Result],
                                                    dataSource: DataSource[F],
                                                    gitConfig: GitConfig)
                                                   (implicit ec: ExecutionContext): Stream[F, Unit] = {
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(gitConfig.requestTimeout).stream
      githubClient <- GithubHttp4sClient.stream(client, gitConfig)
      repository <- GithubPostgresRepository.stream(dataSource)
      team <- githubClient.getUserTeams()
      discussion <- githubClient.getTeamDiscussions(team.id)
      comment <- githubClient.getTeamDiscussionComments(team.id, discussion.number)
      result <- Stream.eval(repository.save(TeamDiscussionCommentDetails(team, discussion, comment)))
      _ <- Stream.eval(queue.enqueue1(result))
    } yield ()
  }
}

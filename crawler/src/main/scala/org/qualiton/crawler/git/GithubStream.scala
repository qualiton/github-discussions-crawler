package org.qualiton.crawler.git

import cats.effect.{ConcurrentEffect, ContextShift}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.git.GithubRepository.TeamDiscussionCommentDetails

import scala.concurrent.ExecutionContext

object GithubStream {

  def apply[F[_] : ConcurrentEffect : ContextShift](dataSource: DataSource[F], gitConfig: GitConfig)(implicit ec: ExecutionContext): Stream[F, Unit] = {
    for {
      client <- BlazeClientBuilder[F](ec).stream
      githubClient <- GithubHttp4sClient.stream(client, gitConfig)
      repository <- GithubPostgresRepository.stream(dataSource)
      team <- githubClient.getUserTeams()
      discussion <- githubClient.getTeamDiscussions(team.id)
      comment <- githubClient.getTeamDiscussionComments(team.id, discussion.number)
      _ <- Stream.eval(repository.save(TeamDiscussionCommentDetails(team, discussion, comment)))
    } yield ()
  }
}

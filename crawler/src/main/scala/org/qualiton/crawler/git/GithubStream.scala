package org.qualiton.crawler.git

import cats.effect.{ConcurrentEffect, Sync}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.qualiton.crawler.common.config.GitConfig

import scala.concurrent.ExecutionContext

object GithubStream {

  def apply[F[_] : ConcurrentEffect](gitConfig: GitConfig)(implicit ec: ExecutionContext): Stream[F, Unit] = {
    for {
      client <- BlazeClientBuilder[F](ec).stream
      githubClient = GithubHttp4sClient(client, gitConfig)
      team <- githubClient.getUserTeams()
      discussion <- githubClient.getTeamDiscussions(team.id)
      comment <- githubClient.getTeamDiscussionComments(team.id, discussion.number)
      _ <- Stream.eval(Sync[F].delay(println(comment)))
    } yield ()
  }
}

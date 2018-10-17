package org.qualiton.crawler.git

import cats.effect.{Effect, Sync}
import fs2.Stream
import org.http4s.client.blaze.Http1Client
import org.qualiton.crawler.common.config.GitConfig

object GithubStream {

  def apply[F[_] : Effect](gitConfig: GitConfig): Stream[F, Unit] = {
    for {
      client <- Http1Client.stream[F]()
      githubClient = GithubHttp4sClient(client, gitConfig)
      team <- githubClient.getUserTeams()
      discussion <- githubClient.getTeamDiscussions(team.id)
      comment <- githubClient.getTeamDiscussionComments(team.id, discussion.number)
      _ <- Stream.eval(Sync[F].delay(println(comment)))
    } yield ()
  }
}

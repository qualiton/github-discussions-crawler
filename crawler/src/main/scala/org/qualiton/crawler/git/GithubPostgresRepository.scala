package org.qualiton.crawler.git

import cats.effect.{ContextShift, Effect, Sync}
import fs2.Stream
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.git.GithubRepository.{CommentAlreadyExists, Result, TeamDiscussionCommentDetails}

class GithubPostgresRepository[F[_] : Effect : ContextShift] private(dataSource: DataSource[F]) extends GithubRepository[F] {

  println(dataSource)

  override def save(teamDiscussionCommentDetails: TeamDiscussionCommentDetails): F[Result] = {
    Sync[F].delay {
      import teamDiscussionCommentDetails._
      println(s"${userTeam.name} >> ${teamDiscussion.title} - $addressees - $channels")
      CommentAlreadyExists
    }
  }
}

object GithubPostgresRepository {
  def stream[F[_] : Effect : ContextShift](dataSource: DataSource[F]): Stream[F, GithubRepository[F]] =
    Stream.eval(Sync[F].delay(new GithubPostgresRepository[F](dataSource)))
}

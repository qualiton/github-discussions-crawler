package org.qualiton.crawler.infrastructure.persistence
package git

import java.time.Instant

import cats.effect.{ ContextShift, Effect, Sync }
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import fs2.Stream

import doobie.Meta
import doobie.free.connection.delay
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.circe.generic.auto._

import org.qualiton.crawler.domain.git.{ Discussion, GithubRepository }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ selectDiscussionQuery, selectLatestUpdatedAt }
import org.qualiton.crawler.infrastructure.persistence.meta.codecMeta

class GithubPostgresRepository[F[_] : Effect : ContextShift] private(transactor: Transactor[F]) extends GithubRepository[F] {

  override def findLastUpdatedAt: F[Instant] =
    selectLatestUpdatedAt.unique
      .transact(transactor)

  override def find(teamId: Long, discussionId: Long): F[Option[Discussion]] =
    selectDiscussionQuery(teamId, discussionId).option
      .transact(transactor)
      .flatMap(_.traverse(GithubPostgresAssembler.toDomain[F]))

  override def save(discussion: Discussion): F[Unit] =
    delay(GithubPostgresAssembler.fromDomain(discussion))
      .flatMap(p => GithubPostgresRepository.insertDiscussionUpdate(p).run.void)
      .transact(transactor)
}

object GithubPostgresRepository {

  def stream[F[_] : Effect : ContextShift](transactor: Transactor[F]): Stream[F, GithubRepository[F]] =
    Stream.eval(Sync[F].delay(new GithubPostgresRepository[F](transactor)))

  final case class DiscussionPersistence(
      teamId: Long,
      teamName: String,
      discussionId: Long,
      title: String,
      author: String,
      avatarUrl: String,
      body: String,
      bodyVersion: String,
      discussionUrl: String,
      commentsListPersistence: CommentsListPersistence,
      createdAt: Instant,
      updatedAt: Instant)


  final case class CommentsListPersistence(comments: List[CommentPersistence])

  final case class CommentPersistence(
      commentId: Long,
      author: String,
      avatarUrl: String,
      body: String,
      bodyVersion: String,
      commentUrl: String,
      createdAt: Instant,
      updatedAt: Instant)

  object CommentsListPersistence {

    implicit val transactionDetailsMeta: Meta[CommentsListPersistence] = codecMeta[CommentsListPersistence]
  }

  def insertDiscussionUpdate(discussionPersistence: DiscussionPersistence): Update0 = {
    import discussionPersistence._
    sql"""
          INSERT INTO discussion (team_id, team_name, discussion_id, title, author, avatar_url, body, body_version, discussion_url, comments, created_at, updated_at)
          VALUES($teamId, $teamName, $discussionId, $title, $author, $avatarUrl, $body, $bodyVersion, $discussionUrl, $commentsListPersistence, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_DISCUSSION DO UPDATE
          SET
              team_name = $teamName,
              title = $title,
              body = $body,
              body_version = $bodyVersion,
              discussion_url = $discussionUrl,
              comments = $commentsListPersistence,
              updated_at = $updatedAt,
              refreshed_at = now()""".update
  }

  def selectDiscussionQuery(teamId: Long, discussionId: Long): Query0[DiscussionPersistence] =
    sql"""
          SELECT team_id, team_name, discussion_id, title, author, avatar_url, body, body_version, discussion_url, comments, created_at, updated_at
          FROM discussion
          WHERE team_id = $teamId AND discussion_id = $discussionId
      """.query[DiscussionPersistence]

  def selectLatestUpdatedAt: Query0[Instant] =
    sql"""
         SELECT COALESCE(MAX(updated_at), (now() - INTERVAL '10 year'))
         FROM discussion
      """.query[Instant]
}

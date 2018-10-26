package org.qualiton.crawler.infrastructure.persistence
package git

import java.time.Instant

import cats.effect.{ ContextShift, Effect, Sync }
import cats.syntax.applicativeError._
import cats.syntax.functor._
import fs2.Stream

import doobie.Meta
import doobie.free.connection.{ delay, ConnectionIO }
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import io.circe.generic.auto._
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.git.{ GithubRepository, TeamDiscussionDetails }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ selectLatestUpdatedAt, selectTeamDiscussionDetailsQuery }
import org.qualiton.crawler.infrastructure.persistence.meta.codecMeta

class GithubPostgresRepository[F[_] : Effect : ContextShift] private(dataSource: DataSource[F]) extends GithubRepository[F] {

  private val transactor = dataSource.hikariTransactor

  override def findLastUpdatedAt: F[Instant] =
    selectLatestUpdatedAt.unique.transact(transactor)

  override def find(teamId: Long, discussionId: Long): F[Either[Throwable, Option[TeamDiscussionDetails]]] = {
    val find: ConnectionIO[Option[TeamDiscussionDetails]] = for {
      maybeDiscussion <- selectTeamDiscussionDetailsQuery(teamId, discussionId).option
      maybeTeamDiscussionDetails <- delay(maybeDiscussion.flatMap(d => GithubPostgresAssembler.toDomain(d).toOption))
    } yield maybeTeamDiscussionDetails

    find.transact(transactor).attempt
  }

  override def save(teamDiscussionDetails: TeamDiscussionDetails): F[Either[Throwable, Unit]] = {
    val save: ConnectionIO[Unit] = for {
      newTeamDiscussion <- delay(GithubPostgresAssembler.toTeamDiscussionDetailsPersistence(teamDiscussionDetails))
      _ <- GithubPostgresRepository.insertTeamDiscussionDetailsUpdate(newTeamDiscussion).run.void
    } yield ()

    save.transact(transactor).attempt
  }
}

object GithubPostgresRepository {

  def stream[F[_] : Effect : ContextShift](dataSource: DataSource[F]): Stream[F, GithubRepository[F]] =
    Stream.eval(Sync[F].delay(new GithubPostgresRepository[F](dataSource)))

  final case class TeamDiscussionDetailsPersistence(
      teamId: Long,
      teamName: String,
      discussionId: Long,
      title: String,
      author: String,
      body: String,
      bodyVersion: String,
      commentsCount: Long,
      url: String,
      comments: CommentsListPersistence,
      createdAt: Instant,
      updatedAt: Instant)


  final case class CommentsListPersistence(comments: List[CommentPersistence])

  final case class CommentPersistence(
      commentId: Long,
      author: String,
      body: String,
      bodyVersion: String,
      url: String,
      createdAt: Instant)

  object CommentsListPersistence {

    implicit val transactionDetailsMeta: Meta[CommentsListPersistence] = codecMeta[CommentsListPersistence]
  }

  def insertTeamDiscussionDetailsUpdate(teamDiscussionDetailsPersistence: TeamDiscussionDetailsPersistence): Update0 = {
    import teamDiscussionDetailsPersistence._
    sql"""
          INSERT INTO discussion (team_id, team_name, discussion_id, title, author, body, body_version, comments_count, url, comments, created_at, updated_at)
          VALUES($teamId, $teamName, $discussionId, $title, $author, $body, $bodyVersion, $commentsCount, $url, $comments, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_DISCUSSION DO UPDATE
          SET
              team_name = $teamName,
              title = $title,
              comments_count = $commentsCount,
              body = $body,
              body_version = $bodyVersion,
              comments_count = $commentsCount,
              url = $url,
              comments = $comments,
              updated_at = $updatedAt,
              refreshed_at = now()""".update
  }

  def selectTeamDiscussionDetailsQuery(teamId: Long, discussionId: Long): Query0[TeamDiscussionDetailsPersistence] =
    sql"""
          SELECT team_id, team_name, discussion_id, title, author, body, body_version, comments_count, url, comments, created_at, updated_at
          FROM discussion
          WHERE team_id = $teamId AND discussion_id = $discussionId
      """.query[TeamDiscussionDetailsPersistence]

  def selectLatestUpdatedAt: Query0[Instant] =
    sql"""
         SELECT COALESCE(MAX(updated_at), (now() - INTERVAL `10 year`))
         FROM discussion
      """.query[Instant]
}

package org.qualiton.crawler.infrastructure.persistence.git

import java.time.Instant

import cats.effect.{ContextShift, Effect, Sync}
import cats.instances.list._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import doobie.free.connection.{ConnectionIO, delay}
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.{Update, Update0}
import fs2.Stream
import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.git.{GithubRepository, TeamDiscussionDetails}

class GithubPostgresRepository[F[_] : Effect : ContextShift] private(dataSource: DataSource[F]) extends GithubRepository[F] {

  private val transactor = dataSource.hikariTransactor

  override def findLastUpdatedAt: F[Option[Instant]] =
    Sync[F].delay {
      Some(Instant.now())
    }

  override def find(teamId: Long, discussionId: Long): F[Either[Throwable, Option[TeamDiscussionDetails]]] = {
    val find: ConnectionIO[Option[TeamDiscussionDetails]] = for {
      maybeDiscussion <- GithubPostgresRepository.selectTeamDiscussionQuery(teamId, discussionId).option
      comments <- GithubPostgresRepository.selectTeamDiscussionCommentsQuery(teamId, discussionId).to[List]
      maybeTeamDiscussionDetails <- delay(maybeDiscussion.flatMap(d => GithubPostgresAssembler.toDomain(d, comments).toOption))
    } yield maybeTeamDiscussionDetails

    find.transact(transactor).attempt
  }

  override def save(teamDiscussionDetails: TeamDiscussionDetails): F[Either[Throwable, Unit]] = {

    import teamDiscussionDetails._

    val save: ConnectionIO[Unit] = for {
      newUserTeam <- delay(GithubPostgresAssembler.toUserTeamPersistence(team))
      newTeamDiscussion <- delay(GithubPostgresAssembler.toTeamDiscussionPersistence(newUserTeam.teamId, discussion))
      newTeamDiscussionComments <-
        delay(comments.map(GithubPostgresAssembler.toTeamDiscussionCommentPersistence(newUserTeam.teamId, newTeamDiscussion.discussionId, _)))
      _ <- GithubPostgresRepository.insertTeamUpdate(newUserTeam).run.void
      _ <- GithubPostgresRepository.insertTeamDiscussionUpdate(newTeamDiscussion).run.void
      _ <- GithubPostgresRepository.insertTeamDiscussionCommentsBatch(newTeamDiscussionComments).void
    } yield ()

    save.transact(transactor).attempt
  }
}

object GithubPostgresRepository {

  def stream[F[_] : Effect : ContextShift](dataSource: DataSource[F]): Stream[F, GithubRepository[F]] =
    Stream.eval(Sync[F].delay(new GithubPostgresRepository[F](dataSource)))

  final case class UserTeamPersistence(teamId: Long,
                                       name: String,
                                       createdAt: Instant,
                                       updatedAt: Instant)

  final case class TeamDiscussionPersistence(teamId: Long,
                                             discussionId: Long,
                                             title: String,
                                             author: String,
                                             body: String,
                                             bodyVersion: String,
                                             commentsCount: Long,
                                             url: String,
                                             createdAt: Instant,
                                             updatedAt: Instant)


  final case class TeamDiscussionCommentPersistence(teamId: Long,
                                                    discussionId: Long,
                                                    commentId: Long,
                                                    author: String,
                                                    body: String,
                                                    bodyVersion: String,
                                                    url: String,
                                                    createdAt: Instant)

  def insertTeamUpdate(userTeamPersistence: UserTeamPersistence): Update0 = {
    import userTeamPersistence._
    sql"""
          INSERT INTO team (team_id, name, created_at, updated_at) VALUES($teamId, $name, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_TEAM DO UPDATE
          SET name = $name, refreshed_at = now()""".update
  }

  def selectTeamDiscussionQuery(teamId: Long, discussionId: Long): Query0[TeamDiscussionPersistence] =
    sql"""
          SELECT team_id, discussion_id, title, author, body, body_version, comments_count, url, created_at, updated_at
          FROM discussion
          WHERE team_id = $teamId AND discussion_id = $discussionId""".query

  def insertTeamDiscussionUpdate(teamDiscussionPersistence: TeamDiscussionPersistence): Update0 = {
    import teamDiscussionPersistence._
    sql"""
          INSERT INTO discussion (team_id, discussion_id, title, author, body, body_version, comments_count, url, created_at, updated_at)
          VALUES($teamId, $discussionId, $title, $author, $body, $bodyVersion, $commentsCount, $url, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_DISCUSSION DO UPDATE
          SET title = $title,
            comments_count = $commentsCount,
            body = $body,
            body_version = $bodyVersion,
            updated_at = $updatedAt,
            refreshed_at = now()""".update
  }

  def selectTeamDiscussionCommentsQuery(teamId: Long, discussionId: Long): Query0[TeamDiscussionCommentPersistence] =
    sql"""
          SELECT team_id, discussion_id, comment_id, author, body, body_version, url, created_at, updated_at
          FROM comment
          WHERE team_id = $teamId AND discussion_id = $discussionId""".query

  def insertTeamDiscussionCommentsBatch(teamDiscussionCommentPersistenceList: List[TeamDiscussionCommentPersistence]): ConnectionIO[Int] = {
    val sql =
      """INSERT INTO comment (team_id, discussion_id, comment_id, author, body, body_version, url, created_at)
         VALUES(?, ?, ?,  ?, ?, ?, ?, ?)
         ON CONFLICT ON CONSTRAINT PK_COMMENT DO UPDATE
         SET body = EXCLUDED.body,
             body_version = EXCLUDED.$bodyVersion,
             refreshed_at = now()"""

    Update[TeamDiscussionCommentPersistence](sql).updateMany(teamDiscussionCommentPersistenceList)
  }

  def selectLatestUpdatedAt(): Query0[Instant] =
    sql"""
         SELECT MAX(updated_at) as updated_at
            (SELECT updated_at
              FROM team
             UNION
             SELECT updated_at
              FROM discussion
             UNION
             SELECT updated_at
              FROM comment) as updated_at""".query[Instant]
}

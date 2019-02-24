package org.qualiton.crawler
package infrastructure.persistence
package git

import java.time.Instant

import cats.data.{ NonEmptyList, OptionT }
import cats.effect.{ ContextShift, Effect }
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import fs2.Stream

import doobie.free.connection.{ delay, ConnectionIO }
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import doobie.util.Read

import org.qualiton.crawler.common.datasource.DataSource
import org.qualiton.crawler.domain.git.{ GithubRepository, TeamDiscussionAggregateRoot, TeamDiscussionId }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ selectLatestUpdatedAt, DiscussionAggregateRootPersistence }

class GithubPostgresRepository[F[_] : Effect : ContextShift] private(transactor: Transactor[F]) extends GithubRepository[F] {

  override def findLastUpdatedAt: F[Instant] =
    selectLatestUpdatedAt.unique
      .transact(transactor)

  override def find(id: TeamDiscussionId): F[Option[TeamDiscussionAggregateRoot]] = {
    val (teamId :: discussionId :: Nil) = id.value.split("_").toList.map(_.toLong)
    val program: OptionT[doobie.ConnectionIO, DiscussionAggregateRootPersistence] = for {
      team <- OptionT(GithubPostgresRepository.selectTeamQuery(teamId).option)
      discussion <- OptionT(GithubPostgresRepository.selectDiscussionQuery(teamId, discussionId).option)
      comments <- OptionT.liftF(GithubPostgresRepository.selectCommentQuery(teamId, discussionId).nel)
    } yield DiscussionAggregateRootPersistence(team, discussion, comments)

    program.value.transact(transactor)
      .flatMap(_.traverse[F, TeamDiscussionAggregateRoot](GithubPostgresAssembler.toDomain[F]))
  }

  override def save(discussion: TeamDiscussionAggregateRoot): F[Unit] = {
    val program = for {
      aggregate <- delay(GithubPostgresAssembler.fromDomain(discussion))
      _ <- GithubPostgresRepository.insertTeamUpdate(aggregate.team).run
      _ <- GithubPostgresRepository.insertDiscussionUpdate(aggregate.discussion).run
      _ <- aggregate.comments.traverse[ConnectionIO, Unit](c => GithubPostgresRepository.insertAuthorUpdate(c.author).run >> GithubPostgresRepository.insertCommentUpdate(c).run.void)
    } yield ()

    program.transact(transactor)
  }
}

object GithubPostgresRepository {

  def stream[F[_] : Effect : ContextShift](dataSource: DataSource[F]): Stream[F, GithubRepository[F]] =
    new GithubPostgresRepository[F](dataSource.hikariTransactor).delay[F].stream

  final case class DiscussionAggregateRootPersistence(
      team: TeamPersistence,
      discussion: DiscussionPersistence,
      comments: NonEmptyList[CommentPersistence])

  final case class TeamPersistence(
      id: Long,
      name: String,
      description: String,
      createdAt: Instant,
      updatedAt: Instant)

  final case class DiscussionPersistence(
      teamId: Long,
      discussionId: Long,
      title: String,
      createdAt: Instant,
      updatedAt: Instant)

  final case class AuthorPersistence(
      id: Long,
      name: String,
      avatarUrl: String)

  final case class CommentPersistence(
      teamId: Long,
      discussionId: Long,
      commentId: Long,
      author: AuthorPersistence,
      url: String,
      body: String,
      bodyVersion: String,
      createdAt: Instant,
      updatedAt: Instant)

  implicit val authorPersistenceRead: Read[AuthorPersistence] =
    Read[(Long, String, String)].map { case (id, name, url) => AuthorPersistence(id, name, url) }

  def insertTeamUpdate(teamPersistence: TeamPersistence): Update0 = {
    import teamPersistence._
    sql"""
          INSERT INTO team (id, name, description, created_at, updated_at)
          VALUES($id, $name, $description, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_TEAM DO UPDATE
          SET
              name = $name,
              description = $description,
              updated_at = $updatedAt,
              refreshed_at = now()""".update
  }

  def insertAuthorUpdate(authorPersistence: AuthorPersistence): Update0 = {
    import authorPersistence._
    sql"""
          INSERT INTO author (id, name, avatar_url)
          VALUES($id, $name, $avatarUrl)
          ON CONFLICT ON CONSTRAINT PK_AUTHOR DO UPDATE
          SET
              name = $name,
              avatar_url = $avatarUrl,
              refreshed_at = now()""".update
  }

  def insertDiscussionUpdate(discussionPersistence: DiscussionPersistence): Update0 = {
    import discussionPersistence._
    sql"""
          INSERT INTO discussion (team_id, discussion_id, title, created_at, updated_at)
          VALUES($teamId, $discussionId, $title, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_DISCUSSION DO UPDATE
          SET
              title = $title,
              updated_at = $updatedAt,
              refreshed_at = now()""".update
  }

  def insertCommentUpdate(commentPersistence: CommentPersistence): Update0 = {
    import commentPersistence._
    sql"""
          INSERT INTO comment (team_id, discussion_id, comment_id, author_id, url, body, body_version, created_at, updated_at)
          VALUES($teamId, $discussionId, $commentId, ${ author.id }, $url, $body, $bodyVersion, $createdAt, $updatedAt)
          ON CONFLICT ON CONSTRAINT PK_COMMENT DO UPDATE
          SET
              author_id = ${ author.id },
              url = $url,
              body = $body,
              body_version = $bodyVersion,
              updated_at = $updatedAt,
              refreshed_at = now()""".update
  }

  def selectTeamQuery(teamId: Long): Query0[TeamPersistence] =
    sql"""
          SELECT id, name, description, created_at, updated_at
          FROM team
          WHERE id = $teamId
      """.query[TeamPersistence]

  def selectDiscussionQuery(teamId: Long, discussionId: Long): Query0[DiscussionPersistence] =
    sql"""
          SELECT team_id, discussion_id, title, created_at, updated_at
          FROM discussion
          WHERE team_id = $teamId AND discussion_id = $discussionId
      """.query[DiscussionPersistence]

  def selectCommentQuery(teamId: Long, discussionId: Long): Query0[CommentPersistence] =
    sql"""
          SELECT c.team_id, c.discussion_id, c.comment_id, a.id, a.name, a.avatar_url, c.url, c.body, c.body_version, c.created_at, c.updated_at
          FROM comment c
          JOIN author a ON a.id = c.author_id
          WHERE c.team_id = $teamId AND c.discussion_id = $discussionId
      """.query[CommentPersistence]

  def selectLatestUpdatedAt: Query0[Instant] =
    sql"""
         SELECT COALESCE(MAX(updated_at), (now() - INTERVAL '10 year'))
         FROM comment
      """.query[Instant]
}

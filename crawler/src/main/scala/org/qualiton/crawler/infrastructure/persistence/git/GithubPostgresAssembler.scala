package org.qualiton.crawler.infrastructure.persistence.git

import cats.data.Validated
import eu.timepit.refined.auto.autoUnwrap
import org.qualiton.crawler.domain.git.{Comment, Discussion, Team, TeamDiscussionDetails}
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{TeamDiscussionCommentPersistence, TeamDiscussionPersistence, UserTeamPersistence}

object GithubPostgresAssembler {

  def toUserTeamPersistence(team: Team): UserTeamPersistence =
    UserTeamPersistence(
      teamId = team.id,
      name = team.name,
      createdAt = team.createdAt,
      updatedAt = team.updatedAt)

  def toTeamDiscussionPersistence(teamId: Long, discussion: Discussion): TeamDiscussionPersistence =
    TeamDiscussionPersistence(
      teamId = teamId,
      discussionId = discussion.id,
      title = discussion.title,
      author = discussion.author,
      body = discussion.body,
      bodyVersion = discussion.bodyVersion,
      commentsCount = discussion.commentsCount,
      url = discussion.url,
      createdAt = discussion.createdAt,
      updatedAt = discussion.updatedAt)

  def toTeamDiscussionCommentPersistence(teamId: Long, discussionId: Long, comment: Comment): TeamDiscussionCommentPersistence =
    TeamDiscussionCommentPersistence(
      teamId = teamId,
      discussionId = discussionId,
      commentId = comment.id,
      author = comment.author,
      body = comment.body,
      bodyVersion = comment.bodyVersion,
      url = comment.url,
      createdAt = comment.createdAt)

  def toDomain(teamDiscussionPersistence: TeamDiscussionPersistence,
               teamDiscussionCommentPersistenceList: List[TeamDiscussionCommentPersistence]): Validated[Throwable, TeamDiscussionDetails] = ???
}

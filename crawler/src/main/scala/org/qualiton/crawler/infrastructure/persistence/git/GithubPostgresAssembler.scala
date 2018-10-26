package org.qualiton.crawler.infrastructure.persistence.git

import cats.data.ValidatedNel
import cats.syntax.apply._
import cats.syntax.either._

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString
import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Discussion, Team, TeamDiscussionDetails, ValidationError }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ CommentPersistence, CommentsListPersistence, TeamDiscussionDetailsPersistence }

object GithubPostgresAssembler {

  def toTeamDiscussionDetailsPersistence(teamDiscussionDetails: TeamDiscussionDetails): TeamDiscussionDetailsPersistence = {
    import teamDiscussionDetails._
    TeamDiscussionDetailsPersistence(
      teamId = team.id,
      teamName = team.name,
      discussionId = discussion.id,
      title = discussion.title,
      author = discussion.author,
      body = discussion.body,
      bodyVersion = discussion.bodyVersion,
      commentsCount = discussion.commentsCount,
      url = discussion.url,
      comments = CommentsListPersistence(comments.map(c => CommentPersistence(
        commentId = c.id,
        author = c.author,
        body = c.body,
        bodyVersion = c.bodyVersion,
        url = c.url,
        createdAt = c.createdAt))),
      createdAt = discussion.createdAt,
      updatedAt = discussion.updatedAt)
  }

  def toDomain(teamDiscussionDetailsPersistence: TeamDiscussionDetailsPersistence): ValidatedNel[Throwable, TeamDiscussionDetails] = {
    import teamDiscussionDetailsPersistence._

    val teamValidated: ValidatedNel[ValidationError, Team] = refineV[NonEmpty](teamName)
      .map(teamName => Team(teamId, teamName))
      .leftMap(ValidationError)
      .toValidatedNel

    val discussionValidated: ValidatedNel[ValidationError, Discussion] = {
      val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](title).leftMap(ValidationError).toValidatedNel
      val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author).leftMap(ValidationError).toValidatedNel
      val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(ValidationError).toValidatedNel
      val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](bodyVersion).leftMap(ValidationError).toValidatedNel
      val urlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](url).leftMap(ValidationError).toValidatedNel

      (titleValidated, authorValidated, bodyValidated, bodyVersionValidated, urlValidated)
        .mapN((title, author, body, bodyVersion, url) =>
          Discussion(discussionId, title, author, body, bodyVersion, url, commentsCount, createdAt, updatedAt))
    }

    //TODO add comments
    (teamValidated, discussionValidated)
      .mapN((team, discussion) => TeamDiscussionDetails(team, discussion, List.empty))
  }
}

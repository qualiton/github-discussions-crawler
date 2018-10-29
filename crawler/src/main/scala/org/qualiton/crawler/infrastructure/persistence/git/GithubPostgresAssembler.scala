package org.qualiton.crawler.infrastructure.persistence.git

import cats.data.{ Validated, ValidatedNel }
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.traverse._

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Comment, Discussion, ValidationError }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ CommentPersistence, CommentsListPersistence, DiscussionPersistence }

//TODO remove static
object GithubPostgresAssembler {

  def fromDomain(discussion: Discussion): DiscussionPersistence = {
    import discussion._
    DiscussionPersistence(
      teamId = discussion.teamId,
      teamName = discussion.teamName,
      discussionId = discussion.discussionId,
      title = discussion.title,
      author = discussion.author,
      avatarUrl = discussion.avatarUrl,
      body = discussion.body,
      bodyVersion = discussion.bodyVersion,
      discussionUrl = discussion.discussionUrl,
      comments = CommentsListPersistence(comments.map(c => CommentPersistence(
        commentId = c.commentId,
        author = c.author,
        avatarUrl = c.avatarUrl,
        body = c.body,
        bodyVersion = c.bodyVersion,
        commentUrl = c.commentUrl,
        createdAt = c.createdAt,
        updatedAt = c.updatedAt))),
      createdAt = discussion.createdAt,
      updatedAt = discussion.updatedAt)
  }

  def toDomain(discussionPersistence: DiscussionPersistence): Validated[Throwable, Discussion] = {
    import discussionPersistence._

    val teamNameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamName).leftMap(ValidationError(_)).toValidatedNel
    val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](title).leftMap(ValidationError(_)).toValidatedNel
    val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author).leftMap(ValidationError(_)).toValidatedNel
    val avatarUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](avatarUrl).leftMap(ValidationError(_)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(ValidationError(_)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](bodyVersion).leftMap(ValidationError(_)).toValidatedNel
    val discussionUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](discussionUrl).leftMap(ValidationError(_)).toValidatedNel

    val commentsValidated: ValidatedNel[ValidationError, List[Comment]] = comments.comments.traverse(toComment)

    val discussionValidated: ValidatedNel[ValidationError, Discussion] =
      (teamNameValidated, titleValidated, authorValidated, avatarUrlValidated, bodyValidated, bodyVersionValidated, discussionUrlValidated, commentsValidated)
        .mapN((teamName, title, author, avatarUrl, body, bodyVersion, discussionUrl, comments) =>
          Discussion(teamId, teamName, discussionId, title, author, avatarUrl, body, bodyVersion, discussionUrl, comments, createdAt, updatedAt))

    discussionValidated.leftMap(n => ValidationError("Cannot assemble discussion!", n.toList))
  }

  private def toComment(commentPersistence: CommentPersistence): ValidatedNel[ValidationError, Comment] = {
    import commentPersistence._

    val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author).leftMap(ValidationError(_)).toValidatedNel
    val avatarUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](avatarUrl).leftMap(ValidationError(_)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(ValidationError(_)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](bodyVersion).leftMap(ValidationError(_)).toValidatedNel
    val commentUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](commentUrl).leftMap(ValidationError(_)).toValidatedNel

    (authorValidated, avatarUrlValidated, bodyValidated, bodyVersionValidated, commentUrlValidated)
      .mapN((author, avatarUrl, body, bodyVersion, commentUrl) =>
        Comment(commentId, author, avatarUrl, body, bodyVersion, commentUrl, createdAt, updatedAt))
  }
}

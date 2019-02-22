package org.qualiton.crawler.infrastructure.persistence.git


import cats.data.{ NonEmptyList, ValidatedNel }
import cats.effect.Sync
import cats.instances.long._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.nonEmptyTraverse._

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Author, Comment, Discussion, Id, Team, TeamDiscussionAggregateRoot, ValidationError }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ AuthorPersistence, CommentPersistence, DiscussionAggregateRootPersistence, DiscussionPersistence, TeamPersistence }

//TODO remove static
object GithubPostgresAssembler {

  def fromDomain(discussionAggregateRoot: TeamDiscussionAggregateRoot): DiscussionAggregateRootPersistence = {
    import discussionAggregateRoot._
    DiscussionAggregateRootPersistence(
      team = TeamPersistence(team.id, team.name, team.description, team.createdAt, team.updatedAt),
      discussion = DiscussionPersistence(team.id, discussion.id, discussion.title, discussion.createdAt, discussion.updatedAt),
      comments =
        discussionAggregateRoot.discussion.comments
          .map(c =>
            CommentPersistence(
              teamId = team.id,
              discussionId = discussion.id,
              commentId = c.id,
              author = AuthorPersistence(c.author.id, c.author.name, c.author.avatarUrl),
              url = c.url,
              body = c.body,
              bodyVersion = c.bodyVersion,
              createdAt = c.createdAt,
              updatedAt = c.updatedAt)))
  }

  def toDomain[F[_] : Sync](discussionAggregateRootPersistence: DiscussionAggregateRootPersistence): F[TeamDiscussionAggregateRoot] = {
    import discussionAggregateRootPersistence._

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](discussion.discussionId).leftMap(e => ValidationError("discussionId:" + e)).toValidatedNel
    val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](discussion.title).leftMap(e => ValidationError("title:" + e)).toValidatedNel

    val teamValidated: ValidatedNel[ValidationError, Team] = toDomainTeam(team)
    val commentsValidated: ValidatedNel[ValidationError, NonEmptyList[Comment]] = comments.nonEmptyTraverse(toDomainComment).map(_.sortBy(_.id.value))

    val discussionValidated: ValidatedNel[ValidationError, TeamDiscussionAggregateRoot] =
      (teamValidated, idValidated, titleValidated, commentsValidated)
        .mapN((team, id, title, comments) =>
          TeamDiscussionAggregateRoot(team, Discussion(id, title, comments, discussion.createdAt, discussion.updatedAt)))

    discussionValidated
      .toEither match {
      case Right(a) => Sync[F].pure(a)
      case Left(e) => Sync[F].raiseError(ValidationError("Cannot assemble discussion!", e.toList))
    }
  }

  private def toDomainTeam(teamPersistence: TeamPersistence): ValidatedNel[ValidationError, Team] = {
    import teamPersistence._

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](id).leftMap(e => ValidationError("teamId:" + e)).toValidatedNel
    val nameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](name).leftMap(e => ValidationError("teamName:" + e)).toValidatedNel

    (idValidated, nameValidated)
      .mapN((id, name) => Team(id, name, description, createdAt, updatedAt))
  }

  private def toDomainComment(commentPersistence: CommentPersistence): ValidatedNel[ValidationError, Comment] = {
    import commentPersistence._

    val authorIdValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](author.id).leftMap(e => ValidationError("authorId:" + e)).toValidatedNel
    val authorNameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author.name).leftMap(e => ValidationError("authorName:" + e)).toValidatedNel
    val authorUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](author.url).leftMap(e => ValidationError("authorUrl:" + e)).toValidatedNel

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](commentId).leftMap(e => ValidationError("commentId:" + e)).toValidatedNel
    val urlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](url).leftMap(e => ValidationError("commentUrl:" + e)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(e => ValidationError("body:" + e)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](bodyVersion).leftMap(e => ValidationError("bodyVersion:" + e)).toValidatedNel

    (idValidated, urlValidated, authorIdValidated, authorNameValidated, authorUrlValidated, bodyValidated, bodyVersionValidated)
      .mapN((id, commentUrl, authorId, authorName, authorUrl, body, bodyVersion) =>
        Comment(id, commentUrl, Author(authorId, authorName, authorUrl), body, bodyVersion, createdAt, updatedAt))
  }
}

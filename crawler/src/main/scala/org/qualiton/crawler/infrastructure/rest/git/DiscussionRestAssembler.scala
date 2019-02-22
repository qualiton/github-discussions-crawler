package org.qualiton.crawler.infrastructure.rest.git

import java.time.Instant

import scala.language.postfixOps

import cats.data.{ NonEmptyList, ValidatedNel }
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.traverse._

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Author, Comment, Discussion, Id, Team, TeamDiscussionAggregateRoot, ValidationError }
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient.{ AuthorEntity, TeamDiscussionCommentsResponse, TeamDiscussionResponse, UserTeamResponse }

object DiscussionRestAssembler {

  def toDomain[F[_] : Sync](
      userTeamResponse: UserTeamResponse,
      teamDiscussionResponse: TeamDiscussionResponse,
      teamDiscussionCommentsResponse: TeamDiscussionCommentsResponse): F[TeamDiscussionAggregateRoot] = {

    val teamValidated: ValidatedNel[ValidationError, Team] = toDomainTeam(userTeamResponse)

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](teamDiscussionResponse.number).leftMap(e => ValidationError("teamDiscussionResponse.number:" + e)).toValidatedNel
    val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.title).leftMap(e => ValidationError("teamDiscussionResponse.title:" + e)).toValidatedNel

    val commentsValidated: ValidatedNel[ValidationError, NonEmptyList[Comment]] = {
      val originalDiscussionCommentValidated: ValidatedNel[ValidationError, Comment] =
        toDomainComment(
          0L,
          teamDiscussionResponse.author,
          teamDiscussionResponse.body,
          teamDiscussionResponse.body_version,
          teamDiscussionResponse.html_url,
          teamDiscussionResponse.created_at,
          teamDiscussionResponse.updated_at)

      val commentsValidated: ValidatedNel[ValidationError, List[Comment]] =
        teamDiscussionCommentsResponse.map(r => (r.number, r.author, r.body, r.body_version, r.html_url, r.created_at, r.updated_at))
          .traverse(toDomainComment _ tupled).map(_.sortBy(_.id.value))

      (originalDiscussionCommentValidated, commentsValidated)
        .mapN((head, tail) => NonEmptyList.of(head, tail: _*))
    }

    val teamDiscussionAggregateRootValidated: ValidatedNel[ValidationError, TeamDiscussionAggregateRoot] =
      (teamValidated, idValidated, titleValidated, commentsValidated)
        .mapN((team, id, title, comments) => TeamDiscussionAggregateRoot(team, Discussion(id, title, comments, teamDiscussionResponse.created_at, teamDiscussionResponse.updated_at)))

    teamDiscussionAggregateRootValidated
      .toEither match {
      case Right(a) => a.pure[F]
      case Left(e) =>
        Sync[F].raiseError(ValidationError("Cannot assemble discussion!", e.toList))
    }
  }

  private def toDomainTeam(userTeamResponse: UserTeamResponse): ValidatedNel[ValidationError, Team] = {
    import userTeamResponse._

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](id).leftMap(e => ValidationError("teamId:" + e)).toValidatedNel
    val nameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](name).leftMap(e => ValidationError("teamName:" + e)).toValidatedNel

    (idValidated, nameValidated)
      .mapN((id, name) => Team(id, name, description, created_at, updated_at))
  }

  private def toDomainComment(number: Long, author: AuthorEntity, body: String, bodyVersion: String, url: String, createdAt: Instant, updatedAt: Instant): ValidatedNel[ValidationError, Comment] = {

    val authorIdValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](author.id).leftMap(e => ValidationError("author.id:" + e)).toValidatedNel
    val authorNameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author.login).leftMap(e => ValidationError("author.login:" + e)).toValidatedNel
    val authorAvatarUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](author.avatar_url).leftMap(e => ValidationError("author.avatar_url:" + e)).toValidatedNel

    val idValidated: ValidatedNel[ValidationError, Id] = refineV[NonNegative](number).leftMap(e => ValidationError("comment.number:" + e)).toValidatedNel
    val urlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](url).leftMap(e => ValidationError("comment.url:" + e)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(e => ValidationError("body:" + e)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](bodyVersion).leftMap(e => ValidationError("bodyVersion:" + e)).toValidatedNel

    (idValidated, urlValidated, authorIdValidated, authorNameValidated, authorAvatarUrlValidated, bodyValidated, bodyVersionValidated)
      .mapN((id, url, authorId, authorName, avatarUrl, body, bodyVersion) =>
        Comment(id, url, Author(authorId, authorName, avatarUrl), body, bodyVersion, createdAt, updatedAt))
  }
}

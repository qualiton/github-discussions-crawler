package org.qualiton.crawler.infrastructure.rest.git

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.traverse._

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString

import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Comment, Discussion, ValidationError }
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient.{ TeamDiscussionComment, TeamDiscussionCommentsResponse, TeamDiscussionResponse, UserTeamResponse }

//TODO remove static
object DiscussionRestAssembler {

  def toDomain[F[_] : Sync](
      userTeamResponse: UserTeamResponse,
      teamDiscussionResponse: TeamDiscussionResponse,
      teamDiscussionCommentsResponse: TeamDiscussionCommentsResponse): F[Discussion] = {

    val teamNameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](userTeamResponse.name).leftMap(ValidationError(_)).toValidatedNel
    val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.title).leftMap(ValidationError(_)).toValidatedNel
    val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.author.login).leftMap(ValidationError(_)).toValidatedNel
    val avatarUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](teamDiscussionResponse.author.avatar_url).leftMap(ValidationError(_)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.body).leftMap(ValidationError(_)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.body_version).leftMap(ValidationError(_)).toValidatedNel
    val discussionUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](teamDiscussionResponse.html_url).leftMap(ValidationError(_)).toValidatedNel

    val commentsValidated: ValidatedNel[ValidationError, List[Comment]] = teamDiscussionCommentsResponse.traverse(toComment)

    val discussionValidated: ValidatedNel[ValidationError, Discussion] =
      (teamNameValidated, titleValidated, authorValidated, avatarUrlValidated, bodyValidated, bodyVersionValidated, discussionUrlValidated, commentsValidated)
        .mapN { (teamName, title, author, avatarUrl, body, bodyVersion, discussionUrl, comments) =>
          val discussionUpdateAt = (teamDiscussionResponse.updated_at :: comments.map(_.updatedAt)).max
          Discussion(userTeamResponse.id, teamName, teamDiscussionResponse.number, title, author, avatarUrl, body, bodyVersion, discussionUrl, comments, teamDiscussionResponse.created_at, discussionUpdateAt)
        }

    discussionValidated
      .toEither match {
      case Right(a) => a.pure[F]
      case Left(e) => Sync[F].raiseError(ValidationError("Cannot assemble discussion!", e.toList))
    }
  }

  private def toComment(teamDiscussionCommentResponse: TeamDiscussionComment): ValidatedNel[ValidationError, Comment] = {
    import teamDiscussionCommentResponse._

    val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](author.login).leftMap(ValidationError(_)).toValidatedNel
    val avatarUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](author.avatar_url).leftMap(ValidationError(_)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body).leftMap(ValidationError(_)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](body_version).leftMap(ValidationError(_)).toValidatedNel
    val commentUrlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](html_url).leftMap(ValidationError(_)).toValidatedNel

    (authorValidated, avatarUrlValidated, bodyValidated, bodyVersionValidated, commentUrlValidated)
      .mapN((author, avatarUrl, body, bodyVersion, commentUrl) =>
        Comment(number, author, avatarUrl, body, bodyVersion, commentUrl, created_at, updated_at))
  }
}

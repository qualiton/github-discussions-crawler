package org.qualiton.crawler.infrastructure.http.git

import cats.data.{ Validated, ValidatedNel }
import cats.syntax.apply._
import cats.syntax.either._

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }
import eu.timepit.refined.types.string.NonEmptyString
import org.qualiton.crawler.domain.core.Url
import org.qualiton.crawler.domain.git.{ Discussion, ValidationError }
import org.qualiton.crawler.infrastructure.http.git.GithubHttp4sClient.{ TeamDiscussionCommentResponse, TeamDiscussionResponse, UserTeamResponse }

//TODO remove static
object DiscussionRestAssembler {

  def toDomain(
      userTeamResponse: UserTeamResponse,
      teamDiscussionResponse: TeamDiscussionResponse,
      teamDiscussionCommentResponse: List[TeamDiscussionCommentResponse]): Validated[Throwable, Discussion] = {

    val teamNameValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](userTeamResponse.name).leftMap(ValidationError(_)).toValidatedNel
    val titleValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.title).leftMap(ValidationError(_)).toValidatedNel
    val authorValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.author.login).leftMap(ValidationError(_)).toValidatedNel
    val bodyValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.body).leftMap(ValidationError(_)).toValidatedNel
    val bodyVersionValidated: ValidatedNel[ValidationError, NonEmptyString] = refineV[NonEmpty](teamDiscussionResponse.body_version).leftMap(ValidationError(_)).toValidatedNel
    val urlValidated: ValidatedNel[ValidationError, Url] = refineV[RefinedUrl](teamDiscussionResponse.html_url).leftMap(ValidationError(_)).toValidatedNel

    //TODO add comments
    println(teamDiscussionCommentResponse)
    val discussionValidated: ValidatedNel[ValidationError, Discussion] =
      (teamNameValidated, titleValidated, authorValidated, bodyValidated, bodyVersionValidated, urlValidated)
        .mapN((teamName, title, author, body, bodyVersion, url) =>
          Discussion(userTeamResponse.id, teamName, teamDiscussionResponse.number, title, author, body, bodyVersion, url, List.empty, teamDiscussionResponse.created_at, teamDiscussionResponse.updated_at))

    discussionValidated.leftMap(n => ValidationError("Cannot assemble discussion!", n.toList))
  }
}

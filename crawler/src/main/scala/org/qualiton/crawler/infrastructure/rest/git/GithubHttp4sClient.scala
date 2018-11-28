package org.qualiton.crawler
package infrastructure.rest.git

import java.time.Instant
import java.util.Base64

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, Effect }
import fs2.Stream

import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.auto.autoUnwrap
import io.circe.Decoder
import io.circe.fs2._
import io.circe.generic.auto._
import org.http4s.{ AuthScheme, Credentials, EntityDecoder, Header, Headers, Method, Request, Uri }
import org.http4s.circe._
import org.http4s.MediaType.application.json
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ Accept, Authorization }

import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.domain.git._
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sClient.{ TeamDiscussionCommentsResponse, TeamDiscussionResponse, UserTeamResponse }

class GithubHttp4sClient[F[_] : Effect] private(
    client: Client[F],
    gitConfig: GitConfig) extends GithubClient[F] with Http4sClientDsl[F] with LazyLogging {

  import gitConfig._

  private val authorization = Authorization(Credentials.Token(AuthScheme.Basic, Base64.getEncoder.encodeToString(s"${ apiToken.value.value }:x-oauth-basic".getBytes)))
  private val previewAcceptHeader = Header("Accept", "application/vnd.github.echo-preview+json")

  private def request(path: String, acceptHeader: Header): Request[F] =
    Request[F](
      method = Method.GET,
      uri = Uri.unsafeFromString(baseUrl).withPath(path).withQueryParam("direction", "asc"),
      headers = Headers(authorization, acceptHeader))

  private def sendReceiveStream[A: Decoder](request: Request[F]): Stream[F, A] = {
    Stream.eval(client.expect[String](request))
      .through(stringArrayParser)
      .through(decoder[F, A])
  }

  private def getUserTeams(): Stream[F, UserTeamResponse] =
    sendReceiveStream[UserTeamResponse](request("/user/teams", Accept(json)))

  private def getTeamDiscussions(teamId: Long): Stream[F, TeamDiscussionResponse] =
    sendReceiveStream[TeamDiscussionResponse](request(s"/teams/$teamId/discussions", previewAcceptHeader))

  private def getTeamDiscussionComments(teamId: Long, discussionId: Long): Stream[F, TeamDiscussionCommentsResponse] = {
    implicit val teamDiscussionComments: EntityDecoder[F, TeamDiscussionCommentsResponse] = jsonOf[F, TeamDiscussionCommentsResponse]
    Stream.eval(client.expect[TeamDiscussionCommentsResponse](request(s"/teams/$teamId/discussions/$discussionId/comments", previewAcceptHeader)))
  }

  def getTeamDiscussionsUpdatedAfter(instant: Instant): Stream[F, Discussion] = {

    def filterDiscussions(discussion: Discussion): Stream[F, Discussion] =
      if ((discussion.updatedAt :: discussion.comments.map(_.updatedAt)).max.isAfter(instant)) {
        logger.info(s"New item found in ${ discussion.teamName } -> ${ discussion.title }")
        Stream.emit(discussion)
      } else {
        Stream.empty.covary
      }

    for {
      team <- getUserTeams()
      discussion <- getTeamDiscussions(team.id)
      comments <- getTeamDiscussionComments(team.id, discussion.number)
      domainDiscussion <- Stream.eval(DiscussionRestAssembler.toDomain(team, discussion, comments))
      filteredDomainDiscussion <- filterDiscussions(domainDiscussion)
    } yield filteredDomainDiscussion
  }
}

object GithubHttp4sClient {

  final case class UserTeamResponse(
      id: Long,
      name: String)

  final case class Author(login: String, avatar_url: String)

  final case class TeamDiscussionResponse(
      title: String,
      number: Long,
      author: Author,
      body: String,
      body_version: String,
      html_url: String,
      created_at: Instant,
      updated_at: Instant)

  type TeamDiscussionCommentsResponse = List[TeamDiscussionComment]

  final case class TeamDiscussionComment(
      author: Author,
      number: Long,
      body: String,
      body_version: String,
      html_url: String,
      created_at: Instant,
      updated_at: Instant)

  def stream[F[_] : ConcurrentEffect](gitConfig: GitConfig)(implicit ec: ExecutionContext): Stream[F, GithubClient[F]] =
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(gitConfig.requestTimeout).stream
      githubClient <- new GithubHttp4sClient[F](client, gitConfig).delay[F].stream
    } yield githubClient
}

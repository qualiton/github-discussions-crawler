package org.qualiton.crawler.infrastructure.http.git

import java.time.Instant
import java.util.Base64

import scala.concurrent.ExecutionContext

import cats.effect.{ ConcurrentEffect, Effect, Sync }
import fs2.Stream

import eu.timepit.refined.auto.autoUnwrap
import io.circe.Decoder
import io.circe.fs2._
import io.circe.generic.auto._
import org.http4s.{ AuthScheme, Credentials, Header, Headers, Method, Request, Uri }
import org.http4s.MediaType.application.json
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ Accept, Authorization }
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.domain.git._
import org.qualiton.crawler.infrastructure.http.git.GithubHttp4sClient.{ TeamDiscussionCommentResponse, TeamDiscussionResponse, UserTeamResponse }

class GithubHttp4sClient[F[_] : Effect] private(client: Client[F], gitConfig: GitConfig) extends GithubClient[F] with Http4sClientDsl[F] {

  import gitConfig._

  private val authorization = Authorization(Credentials.Token(AuthScheme.Basic, Base64.getEncoder.encodeToString(s"${ apiToken.value.value }:x-oauth-basic".getBytes)))
  private val previewAcceptHeader = Header("Accept", "application/vnd.github.echo-preview+json")

  private def request(path: String, acceptHeader: Header): Request[F] =
    Request[F](
      method = Method.GET,
      uri = Uri.unsafeFromString(baseUrl).withPath(path),
      headers = Headers(authorization, acceptHeader))

  private def sendReceive[A: Decoder](request: Request[F]): Stream[F, A] = {
    Stream.eval(Sync[F].point(client))
      .evalMap(_.expect[String](request))
      .through(stringArrayParser)
      .through(decoder[F, A])
  }

  private def getUserTeams(): Stream[F, UserTeamResponse] =
    sendReceive[UserTeamResponse](request("/user/teams", Accept(json)))

  private def getTeamDiscussions(teamId: Long): Stream[F, TeamDiscussionResponse] =
    sendReceive[TeamDiscussionResponse](request(s"/teams/$teamId/discussions", previewAcceptHeader))

  private def getTeamDiscussionComments(teamId: Long, discussionId: Long): Stream[F, TeamDiscussionCommentResponse] =
    sendReceive[TeamDiscussionCommentResponse](request(s"/teams/$teamId/discussions/$discussionId/comments", previewAcceptHeader))

  def getTeamDiscussionsUpdatedAfter(instant: Instant): Stream[F, Either[Throwable, Discussion]] =
    for {
      team <- getUserTeams()
      discussion <- getTeamDiscussions(team.id).filter(_.updated_at isAfter instant)
      comments <- Stream.eval(getTeamDiscussionComments(team.id, discussion.number).compile.toList)
    } yield DiscussionRestAssembler.toDomain(team, discussion, comments).toEither
}

object GithubHttp4sClient {

  final case class UserTeamResponse(
      id: Long,
      name: String)

  final case class Author(login: String)

  final case class TeamDiscussionResponse(
      title: String,
      number: Long,
      author: Author,
      body: String,
      body_version: String,
      html_url: String,
      created_at: Instant,
      updated_at: Instant)

  final case class TeamDiscussionCommentResponse(
      author: Author,
      number: Long,
      body: String,
      body_version: String,
      html_url: String,
      created_at: Instant)

  def stream[F[_] : ConcurrentEffect](gitConfig: GitConfig)(implicit ec: ExecutionContext): Stream[F, GithubClient[F]] =
    for {
      client <- BlazeClientBuilder[F](ec).withRequestTimeout(gitConfig.requestTimeout).stream
      githubClient <- Stream.eval(Sync[F].delay(new GithubHttp4sClient[F](client, gitConfig)))
    } yield githubClient
}

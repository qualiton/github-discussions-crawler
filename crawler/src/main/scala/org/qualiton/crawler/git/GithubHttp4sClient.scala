package org.qualiton.crawler.git

import java.util.Base64

import cats.effect.{Effect, Sync}
import eu.timepit.refined.auto.autoUnwrap
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import io.circe.generic.auto._
import io.circe.java8.time._
import org.http4s.MediaType.application.json
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{Accept, Authorization}
import org.http4s.{AuthScheme, Credentials, Header, Headers, Method, Request, Uri}
import org.qualiton.crawler.common.config.GitConfig
import org.qualiton.crawler.git.GithubClient.{TeamDiscussion, TeamDiscussionComments, UserTeam}

class GithubHttp4sClient[F[_] : Effect] private(client: Client[F], gitConfig: GitConfig) extends GithubClient[F] with Http4sClientDsl[F] {

  import gitConfig._

  private val authorization = Authorization(Credentials.Token(AuthScheme.Basic, Base64.getEncoder.encodeToString(s"${apiToken.value.value}:x-oauth-basic".getBytes)))
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

  override def getUserTeams(): Stream[F, UserTeam] =
    sendReceive[UserTeam](request("/user/teams", Accept(json)))

  override def getTeamDiscussions(teamId: Long): Stream[F, TeamDiscussion] =
    sendReceive[TeamDiscussion](request(s"/teams/$teamId/discussions", previewAcceptHeader))

  override def getTeamDiscussionComments(teamId: Long, discussionNumber: Long): Stream[F, TeamDiscussionComments] =
    sendReceive[TeamDiscussionComments](request(s"/teams/$teamId/discussions/$discussionNumber/comments", previewAcceptHeader))
}

object GithubHttp4sClient {
  def stream[F[_] : Effect](client: Client[F], gitConfig: GitConfig): Stream[F, GithubClient[F]] =
    Stream.eval(Sync[F].delay(new GithubHttp4sClient[F](client, gitConfig)))
}

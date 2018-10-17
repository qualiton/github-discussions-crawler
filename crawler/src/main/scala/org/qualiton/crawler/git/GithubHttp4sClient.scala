package org.qualiton.crawler.git

import java.util.Base64

import cats.effect.{Effect, Sync}
import eu.timepit.refined.auto.autoUnwrap
import fs2.Stream
import io.circe.Decoder
import io.circe.fs2._
import io.circe.generic.auto._
import io.circe.java8.time._
import org.http4s.MediaType.`application/json`
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

  private def sendReceive[A: Decoder](request:Request[F]):Stream[F, A] = {
    Stream.eval(Sync[F].point(client))
      .evalMap(_.expect[String](request))
      .through(stringArrayParser)
      .through(decoder[F, A])
  }

  override def getUserTeams(): Stream[F, UserTeam] = {
    val request =
      Request[F](
        method = Method.GET,
        uri = Uri.unsafeFromString(baseUrl) / "user" / "teams",
        headers = Headers(authorization, Accept(`application/json`)))

    sendReceive[UserTeam](request)
  }

  override def getTeamDiscussions(teamId: Long): Stream[F, TeamDiscussion] = {
    val request =
      Request[F](
        method = Method.GET,
        uri = Uri.unsafeFromString(baseUrl.value + s"/teams/$teamId/discussions"),
        headers = Headers(authorization, previewAcceptHeader))

    sendReceive[TeamDiscussion](request)
  }

  override def getTeamDiscussionComments(teamId: Long, discussionNumber: Long): Stream[F, TeamDiscussionComments] = {
    val request =
      Request[F](
        method = Method.GET,
        uri = Uri.unsafeFromString(baseUrl.value + s"/teams/$teamId/discussions/$discussionNumber/comments"),
        headers = Headers(authorization, previewAcceptHeader))

    sendReceive[TeamDiscussionComments](request)
  }
}

object GithubHttp4sClient {
  def apply[F[_] : Effect](client: Client[F], gitConfig: GitConfig): GithubClient[F] =
    new GithubHttp4sClient[F](client, gitConfig)
}

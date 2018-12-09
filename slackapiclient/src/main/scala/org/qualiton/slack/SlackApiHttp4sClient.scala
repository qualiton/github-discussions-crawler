package org.qualiton.slack

import java.time.Instant

import cats.effect.Effect
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option.none
import fs2.Stream

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.generic.auto._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import monocle.Traversal
import org.http4s.{ AuthScheme, Charset, Credentials, Headers, Method, Request, Uri }
import org.http4s.MediaType.application.json
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ `Content-Type`, Authorization }

import org.qualiton.slack.models.{ Channel, ChatMessage, User }
import org.qualiton.slack.SlackApiClient.{ RtmConnect, RtmStartState, SlackApiClientError }

class SlackApiHttp4sClient[F[_] : Effect] private(client: Client[F], apiToken: NonEmptyString, slackApiUrl: String Refined Url)
  extends SlackApiClient[F]
    with Http4sClientDsl[F] {

  val F = implicitly[Effect[F]]

  private val authorization: Authorization = Authorization(Credentials.Token(AuthScheme.Bearer, apiToken.value))
  private val slackBaseUrl: Uri = Uri.unsafeFromString(slackApiUrl) / "api"

  /** *************************/
  /** *  Channel Endpoints  ***/
  /** *************************/

  override def findChannelById(channelId: String): F[Option[Channel]] = {
    val request = Request[F](
      method = Method.GET,
      uri = slackBaseUrl / "conversations.info" +? ("channel", channelId),
      headers = Headers(authorization))

    client.expect[Json](request)
      .flatMap(json =>
        root.error.string.getOption(json) match {
          case Some(error) if (error == "channel_not_found") => none[Channel].pure[F]
          case Some(error) => F.raiseError(SlackApiClientError(error))
          case None => root.channel.as[Channel].getOption(json).pure[F]
        })
  }

  override def findChannelByName(name: String): F[Option[Channel]] =
    listChannels.filter(_.name == name).compile.toList.map(_.headOption)

  override def listChannels: Stream[F, Channel] = {
    def listChannelsRequest(cursor: Option[String] = None): F[Json] = {
      val request = Request[F](
        method = Method.GET,
        uri = slackBaseUrl / "conversations.list"
              +? ("exclude_archived", "true")
              +? ("types", "public_channel,private_channel")
              +? ("limit", "100")
              +?? ("cursor", cursor),
        headers = Headers(authorization))

      client.expect[Json](request)
        .flatMap(json =>
          root.error.string.getOption(json) match {
            case Some(error) => F.raiseError(SlackApiClientError(error))
            case None => json.pure[F]
          })
    }

    Stream.eval(listChannelsRequest()).flatMap(paginationApiHelper(_, listChannelsRequest, root.channels.each.as[Channel]))
  }

  /** ************************/
  /** **  User Endpoints  ****/
  /** ************************/

  def findUserById(userId: String): F[Option[User]] = {
    val request = Request[F](
      method = Method.GET,
      uri = slackBaseUrl / "users.info" +? ("user", userId),
      headers = Headers(authorization))

    client.expect[Json](request)
      .flatMap(json =>
        root.error.string.getOption(json) match {
          case Some(error) if (error == "user_not_found") => none[User].pure[F]
          case Some(error) => F.raiseError(SlackApiClientError(error))
          case None => root.user.as[User].getOption(json).pure[F]
        })
  }

  def findUserByName(name: String): F[Option[User]] =
    listUsers.filter(_.name == name).compile.toList.map(_.headOption)

  def listUsers: Stream[F, User] = {
    def listUsersRequest(cursor: Option[String] = None): F[Json] = {
      val request = Request[F](
        method = Method.GET,
        uri = slackBaseUrl / "users.list"
              +? ("limit", "100")
              +?? ("cursor", cursor),
        headers = Headers(authorization))

      client.expect[Json](request)
        .flatMap(json =>
          root.error.string.getOption(json) match {
            case Some(error) => F.raiseError(SlackApiClientError(error))
            case None => json.pure[F]
          })
    }

    Stream.eval(listUsersRequest()).flatMap(paginationApiHelper(_, listUsersRequest, root.members.each.as[User]))
  }

  /** **********************/
  /** **  IM Endpoints  ****/
  /** **********************/

  override def openIm(userId: String): F[Option[String]] = {
    val request = Request[F](
      method = Method.POST,
      uri = slackBaseUrl / "im.open" +? ("user", userId),
      headers = Headers(authorization))

    client.expect[Json](request)
      .flatMap(json =>
        root.error.string.getOption(json) match {
          case Some(error) if (error == "user_not_found") => none[String].pure[F]
          case Some(error) => F.raiseError(SlackApiClientError(error))
          case None => root.channel.id.string.getOption(json).pure[F]
        })
  }

  override def closeIm(channelId: String): F[Unit] = {
    val request = Request[F](
      method = Method.POST,
      uri = slackBaseUrl / "im.close" +? ("channel", channelId),
      headers = Headers(authorization))

    client.expect[Json](request)
      .flatMap[Json](json =>
      root.error.string.getOption(json) match {
        case Some(error) => F.raiseError(SlackApiClientError(error))
        case None => json.pure[F]
      }).void
  }

  /** ************************/
  /** **  Chat Endpoints  ****/
  /** ************************/

  override def postChatMessage(channelId: String, chatMessage: ChatMessage): F[Instant] = {
    val request = Request[F](
      method = Method.POST,
      uri = slackBaseUrl / "chat.postMessage",
      headers = Headers(authorization))
      .withEntity(("channel" -> Json.fromString(channelId)) +: chatMessage.asJsonObject)
      .withContentType(`Content-Type`(json, Charset.`UTF-8`))

    client.expect[Json](request)
      .flatMap(json =>
        root.error.string.getOption(json) match {
          case Some(error) => F.raiseError(SlackApiClientError(error))
          case None =>
            root.ts.as[String].getOption(json)
              .map(i => Instant.ofEpochSecond(i.toDouble.toLong)).get.pure[F]
        }
      )
  }

  /** ***********************/
  /** **  RTM Endpoints  ****/
  /** ***********************/

  override def startRealTimeMessageSession: F[RtmStartState] = {
    val request = Request[F](
      method = Method.GET,
      uri = slackBaseUrl / "rtm.start",
      headers = Headers(authorization))

    client.expect[RtmStartState](request)
  }

  override def connectRealTimeMessageSession: F[RtmConnect] = {
    val request = Request[F](
      method = Method.GET,
      uri = slackBaseUrl / "rtm.connect",
      headers = Headers(authorization))

    client.expect[RtmConnect](request)
  }

  /** ***************************/
  /** **  Private Helpers  ****/
  /** ***************************/

  private def paginationApiHelper[A](result: Json, apiCall: Option[String] => F[Json], itemTraversal: Traversal[Json, A]): Stream[F, A] = {
    val previousStream = Stream.emits(itemTraversal.getAll(result)).covary[F]
    //An empty, null, or non-existent next_cursor in the response indicates no further results.
    root.response_metadata.next_cursor.string.getOption(result).fold(previousStream) { nextCursor =>
      if (nextCursor.isEmpty) {
        previousStream
      } else {
        previousStream ++ Stream.eval(apiCall(Some(nextCursor))).flatMap(paginationApiHelper(_, apiCall, itemTraversal))
      }
    }
  }
}

object SlackApiHttp4sClient {

  def stream[F[_] : Effect](
      client: Client[F],
      apiToken: NonEmptyString,
      slackApiUrl: String Refined Url = SlackApiClient.defaultSlackApiUrl): Stream[F, SlackApiClient[F]] =
    Stream.eval(Effect[F].delay(new SlackApiHttp4sClient(client, apiToken, slackApiUrl)))

}

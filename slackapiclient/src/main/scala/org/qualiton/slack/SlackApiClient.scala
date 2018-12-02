package org.qualiton.slack

import java.time.Instant

import fs2.Stream

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url
import io.circe.Json

import org.qualiton.slack.models.{ Channel, ChatMessage, Group, Im, Team, User }
import org.qualiton.slack.SlackApiClient.RtmStartState

trait SlackApiClient[F[_]] {

  /** *************************/
  /** *  Channel Endpoints  ***/
  /** *************************/

  def findChannelById(channelId: String): F[Option[Channel]]

  def findChannelByName(name: String): F[Option[Channel]]

  def listChannels: Stream[F, Channel]

  /** ************************/
  /** **  User Endpoints  ****/
  /** ************************/

  def findUserById(userId: String): F[Option[User]]

  def findUserByName(name: String): F[Option[User]]

  def listUsers: Stream[F, User]

  def setUserPresence(state: String): F[Unit]

  /** **********************/
  /** **  IM Endpoints  ****/
  /** **********************/

  def openIm(userId: String): F[Option[String]]

  def closeIm(channelId: String): F[Unit]

  /** ************************/
  /** **  Chat Endpoints  ****/
  /** ************************/

  def postChatMessage(channelId: String, chatMessage: ChatMessage): F[Instant]

  /** ***********************/
  /** **  RTM Endpoints  ****/
  /** ***********************/

  def startRealTimeMessageSession: F[RtmStartState]

}

object SlackApiClient {

  val defaultSlackApiUrl: String Refined Url = "https://slack.com/api/"

  case class SlackApiClientError(message: String) extends Exception(message)

  case class RtmStartState(
      url: String,
      self: User,
      team: Team,
      users: Seq[User],
      channels: Seq[Channel],
      groups: Seq[Group],
      ims: Seq[Im],
      bots: Seq[Json]
  )
}

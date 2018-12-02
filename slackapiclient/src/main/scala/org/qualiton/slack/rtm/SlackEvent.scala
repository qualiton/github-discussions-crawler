package org.qualiton.slack.rtm

package slack.models

import org.qualiton.slack.models.{ Channel, Im, User }

sealed trait SlackEvent

case class Hello(
    `type`: String) extends SlackEvent

case class ChannelCreated(
    channel: Channel) extends SlackEvent

case class ChannelDeleted(
    channel: String) extends SlackEvent

case class ChannelRename(
    channel: Channel) extends SlackEvent

case class ImCreated(
    user: String,
    channel: Im) extends SlackEvent

case class ImOpened(
    user: String,
    channel: String) extends SlackEvent

case class ImClose(
    user: String,
    channel: String) extends SlackEvent

case class UserChange(
    user: User) extends SlackEvent

case class TeamJoin(
    user: User) extends SlackEvent

case class Pong(
    `type`: String,
    reply_to: Long) extends SlackEvent



































































































































































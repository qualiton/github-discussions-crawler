package org.qualiton.slack.rtm

package slack.models

import org.qualiton.slack.models.{ Channel, Im, User }

sealed trait SlackEvent

case class Hello(
    `type`: String = "hello") extends SlackEvent

case class Message(
    ts: String,
    channel: String,
    user: Option[String],
    bot_id: Option[String],
    username: Option[String],
    text: String,
    is_starred: Option[Boolean],
    thread_ts: Option[String]) extends SlackEvent

case class ChannelCreated(
    channel: Channel) extends SlackEvent

case class ChannelDeleted(
    channel: String) extends SlackEvent

case class ChannelRename(
    channel: Channel) extends SlackEvent

case class ChannelArchive(
    channel: String,
    user: String) extends SlackEvent

case class ChannelUnarchive(
    channel: String,
    user: String) extends SlackEvent

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

case class UserTyping(
    channel: String,
    user: String) extends SlackEvent

case class BotAdded(
    bot: Bot) extends SlackEvent

case class BotChanged(
    bot: Bot) extends SlackEvent

case class Bot(
    id: String,
    app_id: String,
    name: String)

case class DndUpdatedUser(
    user: String,
    dnd_status: DndStatus,
    event_ts: String) extends SlackEvent

case class DndStatus(
    dnd_enabled: Boolean,
    next_dnd_start_ts: Long,
    next_dnd_end_ts: Long)

case class SubteamMembersChanged(
    subteam_id: String,
    team_id: String) extends SlackEvent

case class SubteamUpdated(
    `type`: String) extends SlackEvent

case class CommandsChanged(
    event_ts: String) extends SlackEvent

case class AppsChanged(
    app: App,
    event_ts: String
) extends SlackEvent

case class App(
    id: String,
    name: String)

case class EmojiChanged(
    event_ts: String) extends SlackEvent

case class Pong(
    reply_to: Long,
    `type`: String = "pong") extends SlackEvent

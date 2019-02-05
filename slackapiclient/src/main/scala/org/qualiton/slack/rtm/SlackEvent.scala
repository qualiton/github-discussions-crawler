package org.qualiton.slack.rtm

package slack.models

import org.qualiton.slack.models.{ Bot, Channel, DndStatus, Im, ReactionItem, User }

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

case class EditMessage(
    user: String,
    text: String,
    ts: String)

case class ReplyMarker(
    user: String,
    ts: String)

case class ReplyMessage(
    user: String,
    text: String,
    thread_ts: String,
    reply_count: Int,
    replies: Seq[ReplyMarker])

case class MessageChanged(
    message: EditMessage,
    previous_message: EditMessage,
    ts: String,
    event_ts: String,
    channel: String) extends SlackEvent

case class MessageDeleted(
    ts: String,
    deleted_ts: String,
    event_ts: String,
    channel: String) extends SlackEvent

case class MessageReplied(
    ts: String,
    event_ts: String,
    channel: String,
    message: ReplyMessage) extends SlackEvent

case class ChannelMarked(
    channel: String,
    ts: String) extends SlackEvent

case class ChannelCreated(
    channel: Channel) extends SlackEvent

case class ChannelJoined(
    channel: Channel) extends SlackEvent

case class ChannelLeft(
    channel: String) extends SlackEvent

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

case class ChannelHistoryChanged(
    latest: Long,
    ts: String,
    event_ts: String) extends SlackEvent

case class ImCreated(
    user: String,
    channel: Im
) extends SlackEvent

case class ImOpened(
    user: String,
    channel: String) extends SlackEvent

case class ImClose(
    user: String,
    channel: String) extends SlackEvent

case class ImMarked(
    channel: String,
    ts: String) extends SlackEvent

case class ImHistoryChanged(
    latest: Long,
    ts: String,
    event_ts: String) extends SlackEvent

case class GroupJoined(
    channel: Channel) extends SlackEvent

case class MpImJoined(
    channel: Channel) extends SlackEvent

case class MpImOpen(
    user: String,
    channel: String,
    event_ts: String) extends SlackEvent

case class MpImClose(
    user: String,
    channel: String,
    event_ts: String,
    converted_to: Option[String]) extends SlackEvent

case class GroupLeft(
    channel: String) extends SlackEvent

case class GroupOpen(
    user: String,
    channel: String) extends SlackEvent

case class GroupClose(
    user: String,
    channel: String) extends SlackEvent

case class GroupArchive(
    channel: String) extends SlackEvent

case class GroupUnarchive(
    channel: String) extends SlackEvent

case class GroupRename(
    channel: Channel) extends SlackEvent

case class GroupMarked(
    channel: String,
    ts: String) extends SlackEvent

case class GroupHistoryChanged(
    latest: Long,
    ts: String,
    event_ts: String) extends SlackEvent

case class FileCreated(
    file_id: String) extends SlackEvent

case class FileShared(
    file_id: String) extends SlackEvent

case class FileUnshared(
    file_id: String) extends SlackEvent

case class FilePublic(
    file_id: String) extends SlackEvent

case class FilePrivate(
    file: String) extends SlackEvent

case class FileChange(
    file_id: String) extends SlackEvent

case class FileDeleted(
    file_id: String,
    event_ts: String) extends SlackEvent

case class FileCommentAdded(
    file_id: String) extends SlackEvent

case class FileCommentEdited(
    file_id: String) extends SlackEvent

case class FileCommentDeleted(
    file_id: String,
    comment: String) extends SlackEvent

case class UserChange(
    user: User) extends SlackEvent

case class TeamJoin(
    user: User) extends SlackEvent

case class ReactionAdded(
    reaction: String,
    item: ReactionItem,
    event_ts: String,
    user: String,
    item_user: Option[String]) extends SlackEvent

case class ReactionRemoved(
    reaction: String,
    item: ReactionItem,
    event_ts: String,
    user: String,
    item_user: Option[String]) extends SlackEvent

case class UserTyping(
    channel: String,
    user: String) extends SlackEvent

case class BotAdded(
    bot: Bot) extends SlackEvent

case class BotChanged(
    bot: Bot) extends SlackEvent

case class DndUpdatedUser(
    user: String,
    dnd_status: DndStatus,
    event_ts: String) extends SlackEvent

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

case class EmojiChanged(
    event_ts: String) extends SlackEvent

case class Pong(
    reply_to: Long,
    `type`: String = "pong") extends SlackEvent

package org.qualiton.slack.models

case class ChatMessage(
    text: Option[String],
    attachments: List[Attachment] = List.empty)

final case class Attachment(
    pretext: String,
    color: String,
    author_name: String,
    author_icon: String,
    title: String,
    title_link: String,
    fields: List[Field],
    ts: Long)

final case class Field(
    title: String,
    value: String,
    short: Boolean)



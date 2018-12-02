package org.qualiton.slack.models

case class ChatMessage(
    text: String,
    attachments: List[Attachment] = List.empty)

final case class Attachment(
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



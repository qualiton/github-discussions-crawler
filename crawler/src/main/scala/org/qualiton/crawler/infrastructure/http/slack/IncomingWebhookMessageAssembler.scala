package org.qualiton.crawler.infrastructure.http.slack

import eu.timepit.refined.auto.autoUnwrap
import org.qualiton.crawler.domain.core.{Event, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent}
import org.qualiton.crawler.infrastructure.http.slack.SlackHttp4sClient.{Attachment, Color, Field, IncomingWebhookMessage}

object IncomingWebhookMessageAssembler {

  def fromDomain(event: Event): IncomingWebhookMessage = event match {
    case NewDiscussionDiscoveredEvent(teamName, title, author, url, totalCommentsCount, addressees, createdAt) =>

      IncomingWebhookMessage(List(Attachment(
        color = Color.Good.entryName,
        author_name = author,
        title = "New discussion has been discovered",
        title_link = url,
        text = title,
        fields =
          List(Field("Team", teamName, true)) :::
            (if (totalCommentsCount > 0) List(Field("Comments", totalCommentsCount.toString, true)) else List.empty) :::
            List(Field("Action needed", addressees.map(_.value).mkString(", "), false)),
        ts = createdAt.getEpochSecond)))

    case NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, comments) =>

      IncomingWebhookMessage(List(Attachment(
        color = Color.Good.entryName,
        author_name = comments.map(_.author.value).toList.mkString(", "),
        title = if (totalCommentsCount == 1) "New comment has been discovered" else s"${comments.size} new comments have been discovered",
        title_link = comments.last.url,
        text = title,
        fields =
          List(
            Field("Team", teamName, true),
            Field("Comments", totalCommentsCount.toString, true),
            Field("Action needed", Set(comments.map(_.addressees.map(_.value))).mkString(", "), false)),
        ts = comments.last.createdAt.getEpochSecond)))
  }
}

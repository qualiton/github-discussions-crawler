package org.qualiton.crawler.infrastructure.http.slack

import eu.timepit.refined.auto.autoUnwrap
import org.qualiton.crawler.domain.core.{ Event, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent }
import org.qualiton.crawler.infrastructure.http.slack.SlackHttp4sClient.{ Attachment, Color, Field, IncomingWebhookMessage }

//TODO remove static
object IncomingWebhookMessageAssembler {

  def fromDomain(event: Event): IncomingWebhookMessage = event match {
    case NewDiscussionDiscoveredEvent(teamName, title, author, avatarUrl, discussionUrl, totalCommentsCount, addressees, createdAt) =>

      IncomingWebhookMessage(List(Attachment(
        pretext = "New discussion has been discovered",
        color = Color.Good.code,
        author_name = author,
        author_icon = avatarUrl,
        title = title,
        title_link = discussionUrl,
        fields =
          List(Field("Team", teamName, true)) :::
          (if (totalCommentsCount > 0) List(Field("Comments", totalCommentsCount.toString, true)) else List.empty) :::
          (if (addressees.nonEmpty) List(Field("Action needed", addressees.map(_.value).mkString(", "), false)) else List.empty),
        ts = createdAt.getEpochSecond)))

    case NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, newComments, createdAt) =>

      val addressees = Set(newComments.map(_.addressees.map(_.value)))

      IncomingWebhookMessage(List(Attachment(
        pretext = if (newComments.size == 1) "New comment has been discovered" else s"${ newComments.size } new comments have been discovered",
        color = Color.Good.code,
        author_name = newComments.map(_.author.value).toList.mkString(", "),
        author_icon = newComments.last.avatarUrl,
        title = title,
        title_link = newComments.last.commentUrl,
        fields =
          List(
            Field("Team", teamName, true),
            Field("Comments", totalCommentsCount.toString, true)) :::
          (if (addressees.nonEmpty) List(Field("Action needed", addressees.mkString(", "), false)) else List.empty),
        ts = createdAt.getEpochSecond)))
  }
}

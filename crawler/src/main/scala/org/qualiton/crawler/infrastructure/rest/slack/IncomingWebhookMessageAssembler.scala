package org.qualiton.crawler.infrastructure.rest.slack

import eu.timepit.refined.auto.autoUnwrap

import org.qualiton.crawler.domain.core.{ Event, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent }
import org.qualiton.crawler.infrastructure.rest.slack.SlackHttp4sClient.{ Attachment, Color, Field, IncomingWebhookMessage }

//TODO remove static
object IncomingWebhookMessageAssembler {

  def fromDomain(event: Event): IncomingWebhookMessage = event match {
    case NewDiscussionDiscoveredEvent(teamName, title, author, avatarUrl, discussionUrl, totalCommentsCount, targeted, createdAt) =>

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
          (if (targeted.nonEmpty) List(Field("Targeted", targeted.map(_.value).mkString(", "), false)) else List.empty),
        ts = createdAt.getEpochSecond)))

    case NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, newComments, createdAt) =>

      val targeted: Set[String] = newComments.foldLeft(Set.empty[String])(_ ++ _.targeted.map(_.value))

      IncomingWebhookMessage(List(Attachment(
        pretext = if (newComments.size == 1) "New comment has been discovered" else s"${ newComments.size } new comments have been discovered",
        color = Color.Good.code,
        author_name = newComments.map(_.author.value).toList.toSet.mkString(", "),
        author_icon = newComments.last.avatarUrl,
        title = title,
        title_link = newComments.last.commentUrl,
        fields =
          List(
            Field("Team", teamName, true),
            Field("Comments", totalCommentsCount.toString, true)) :::
          (if (targeted.nonEmpty) List(Field("Targeted", targeted.mkString(", "), false)) else List.empty),
        ts = createdAt.getEpochSecond)))
  }
}

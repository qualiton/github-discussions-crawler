package org.qualiton.crawler.infrastructure.rest.slack

import eu.timepit.refined.auto.autoUnwrap

import org.qualiton.crawler.domain.core.{ DiscussionEvent, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent }
import org.qualiton.slack.models.{ Attachment, ChatMessage, Field }

object ChatMessageAssembler {

  def fromDomain(event: DiscussionEvent): ChatMessage = event match {
    case NewDiscussionDiscoveredEvent(teamName, title, author, avatarUrl, discussionUrl, totalCommentsCount, targeted, createdAt) =>

      val pretext = if (totalCommentsCount > 1) s"New discussion has been discovered with $totalCommentsCount comments" else "New discussion has been discovered"

      ChatMessage(
        None,
        List(Attachment(
          pretext = pretext,
          color = "good",
          author_name = author,
          author_icon = avatarUrl,
          title = title,
          title_link = discussionUrl,
          fields =
            List(Field("Team", teamName, true)) :::
            (if (totalCommentsCount > 0) List(Field("Comments", totalCommentsCount.toString, true)) else List.empty) :::
            (if (!targeted.isEmpty) List(Field("Targeted", (targeted.persons.map(_.value).toList.sorted ::: targeted.teams.map(_.value).toList.sorted).distinct.mkString(", "), false)) else List.empty),
          ts = createdAt.getEpochSecond)))

    case n@NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, newComments, createdAt) =>

      val targeted: List[String] = n.targeted.persons.map(_.value).toList ::: n.targeted.teams.map(_.value).toList
      val pretext = if (newComments.size == 1) "New comment has been discovered" else s"${ newComments.size } new comments have been discovered"

      ChatMessage(
        None,
        List(Attachment(
          pretext = pretext,
          color = "good",
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

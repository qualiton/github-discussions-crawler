package org.qualiton.crawler.infrastructure.http.slack

import eu.timepit.refined.auto.autoUnwrap
import org.qualiton.crawler.domain.core.{Event, NewCommentAddedEvent, NewDiscussionCreatedEvent}
import org.qualiton.crawler.infrastructure.http.slack.SlackHttp4sClient.{Attachment, Color, Field, IncomingWebhookMessage}

object IncomingWebhookMessageAssembler {

  def fromDomain(event: Event): IncomingWebhookMessage = event match {
    case NewDiscussionCreatedEvent(author, title, link, teamName, addresseeList, createdAt) =>

      IncomingWebhookMessage(List(Attachment(
        color = Color.Good.entryName,
        author_name = author,
        title = "New discussion has been opened",
        title_link = link,
        text = title,
        fields =
          List(
            Field("Team", teamName, true),
            Field("Action needed", addresseeList.map(_.value).mkString(", "), false)),
        ts = createdAt.getEpochSecond)))

    case NewCommentAddedEvent(author, title, link, teamName, numberOfComments, addresseeList, createdAt) =>

      IncomingWebhookMessage(List(Attachment(
        color = Color.Good.entryName,
        author_name = author,
        title = "New comment has been added",
        title_link = link,
        text = title,
        fields =
          List(
            Field("Team", teamName, true),
            Field("Comments", numberOfComments.toString, true),
            Field("Action needed", addresseeList.map(_.value).mkString(", "), false)),
        ts = createdAt.getEpochSecond)))

  }

  val newDiscussion =
    """
      |{
      |    "attachments": [
      |        {
      |            "color": "good",
      |            "author_name": "klachata",
      |            "title": "New discussion has been opened",
      |            "title_link": "https://github.com/orgs/ovotech/teams/boost-vibe/discussions/11",
      |            "text": "Meeting about Migration service kafka messages",
      |            "fields": [
      |                {
      |                    "title": "Team",
      |                    "value": "Boost VIBE",
      |                    "short": true
      |                },
      |				         {
      |                    "title": "Action needed",
      |                    "value": "@asalvadore, @lachatak",
      |                    "short": false
      |                }
      |            ],
      |            "ts": 123456789
      |        }
      |    ]
      |}
    """.stripMargin

  val commentAdded =
    """
      |{
      |    "attachments": [
      |        {
      |            "color": "good",
      |			       "author_name": "klachata",
      |            "title": "New comment has been added",
      |            "text": "Meeting about Migration service kafka messages",
      |            "fields": [
      |                {
      |                    "title": "Team",
      |                    "value": "Boost VIBE",
      |                    "short": true
      |                },
      |				         {
      |                    "title": "Comments",
      |                    "value": 22,
      |                    "short": true
      |                },
      |                {
      |                    "title": "Action needed",
      |                    "value": "@asalvadore, @lachatak",
      |                    "short": false
      |                }
      |            ],
      |            "ts": 123456789
      |        }
      |    ]
      |}
    """.stripMargin
}

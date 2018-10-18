package org.qualiton.crawler.slack

import org.qualiton.crawler.git.GithubRepository.Result
import org.qualiton.crawler.slack.SlackClient.IncomingWebhookMessage

object IncomingWebhookMessageAssembler {

  def toIncomingWebhookMessage(result: Result): IncomingWebhookMessage = ???

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

  val discussionDeleted =
    """
      |{
      |    "attachments": [
      |        {
      |            "color": "warning",
      |			       "author_name": "klachata",
      |            "title": "Discussion has been deleted",
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

  val commentRemove =
    """
      |{
      |    "attachments": [
      |        {
      |            "color": "warning",
      |			       "author_name": "klachata",
      |            "title": "Comment has been removed",
      |            "text": "Meeting about Migration service kafka messages",
      |			        "fields": [
      |                {
      |                    "title": "Team",
      |                    "value": "Boost VIBE",
      |                    "short": true
      |                },
      |				         {
      |                    "title": "Comments",
      |                    "value": 22,
      |                    "short": true
      |                }
      |            ],
      |			"ts": 123456789
      |        }
      |    ]
      |}
    """.stripMargin

}

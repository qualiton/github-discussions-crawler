package org.qualiton.crawler.git

import java.time.Instant

import fs2.Stream
import org.qualiton.crawler.git.GithubClient.{TeamDiscussion, TeamDiscussionComments, UserTeam}

trait GithubClient[F[_]] {

  def getUserTeams(): Stream[F, UserTeam]

  def getTeamDiscussions(teamId: Long): Stream[F, TeamDiscussion]

  def getTeamDiscussionComments(teamId: Long, discussionNumber: Long): Stream[F, TeamDiscussionComments]
}

object GithubClient {

  final case class UserTeam(name: String, id: Long)

  final case class Author(login: String)

  final case class TeamDiscussion(title: String, number: Long, body: String, author: Author, comments_count: Long, html_url: String, created_at: Instant, last_edited_at: Option[Instant])

  final case class TeamDiscussionComments(author: Author, body: String, html_url: String, created_at: Instant, last_edited_at: Option[Instant])

}

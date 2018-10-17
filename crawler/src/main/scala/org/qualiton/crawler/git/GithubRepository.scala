package org.qualiton.crawler.git

import org.qualiton.crawler.git.GithubClient.{TeamDiscussion, TeamDiscussionComments, UserTeam}
import org.qualiton.crawler.git.GithubRepository.{SaveResult, TeamDiscussionCommentDetails}

trait GithubRepository[F[_]] {

  def save(teamDiscussionCommentDetails: TeamDiscussionCommentDetails): F[SaveResult]
}

object GithubRepository {

  final case class TeamDiscussionCommentDetails(userTeam: UserTeam,
                                                teamDiscussion: TeamDiscussion,
                                                teamDiscussionComments: TeamDiscussionComments) {

    private val Addressee = "(@[0-9a-zA-Z]+)".r
    private val Channel = """(#[a-z_\\-]+)""".r

    import teamDiscussionComments._

    val addressees = Addressee.findAllMatchIn(body).toList.map(_.group(1))
    val channels = Channel.findAllMatchIn(body).toList.map(_.group(1))
  }

  sealed trait SaveResult

  object CommentAlreadyExists extends SaveResult

  final case class NewDiscussionCreated() extends SaveResult

  final case class NewCommentAdded() extends SaveResult

}

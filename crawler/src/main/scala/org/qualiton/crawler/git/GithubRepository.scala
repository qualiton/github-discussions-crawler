package org.qualiton.crawler.git

import org.qualiton.crawler.git.GithubClient.{TeamDiscussion, TeamDiscussionComments, UserTeam}
import org.qualiton.crawler.git.GithubRepository.{Result, TeamDiscussionCommentDetails}

trait GithubRepository[F[_]] {

  def save(teamDiscussionCommentDetails: TeamDiscussionCommentDetails): F[Result]
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

  sealed trait Result

  object CommentAlreadyExists extends Result

  final case class NewDiscussionCreated() extends Result

  final case class DiscussionDeleted() extends Result

  final case class NewCommentAdded() extends Result

  final case class CommentRemoved() extends Result

}

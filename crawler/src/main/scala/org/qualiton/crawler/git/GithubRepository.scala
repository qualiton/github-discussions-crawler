package org.qualiton.crawler.git

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Uri}
import org.qualiton.crawler.git.GithubClient.{TeamDiscussion, TeamDiscussionComments, UserTeam}
import org.qualiton.crawler.git.GithubRepository.{Result, TeamDiscussionCommentDetails}
import shapeless.{Witness => W}

trait GithubRepository[F[_]] {

  def save(teamDiscussionCommentDetails: TeamDiscussionCommentDetails): F[Result]
}

object GithubRepository {

  final case class TeamDiscussionCommentDetails(userTeam: UserTeam,
                                                teamDiscussion: TeamDiscussion,
                                                teamDiscussionComments: TeamDiscussionComments)

  type Addressee = String Refined (MatchesRegex[W.`"@[a-z]+"`.T])

  sealed trait Result

  final case class NewDiscussionCreated(author: String,
                                        title: String,
                                        link: String Refined Uri,
                                        teamName: String,
                                        addressee: List[Addressee],
                                        createdAt: Instant) extends Result

  object DiscussionAlreadyExists extends Result

  final case class NewCommentAdded(author: String,
                                   title: String,
                                   link: String Refined Uri,
                                   teamName: String,
                                   numberOfComments: Int,
                                   addressee: List[Addressee],
                                   createdAt: Instant) extends Result

  object CommentAlreadyExists extends Result

  object CommentRemoved extends Result

}

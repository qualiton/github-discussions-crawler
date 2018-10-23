package org.qualiton.crawler.domain.git

import _root_.cats.data.NonEmptyList
import _root_.cats.syntax.option._
import cats.effect.Sync
import eu.timepit.refined._
import org.qualiton.crawler.domain.core._

object EventGenerator {

  def generateEvent[F[_] : Sync](maybePrevious: Option[TeamDiscussionDetails], current: TeamDiscussionDetails): F[Option[Event]] =
    Sync[F].delay {

      def generateNewDiscussionDiscoveredEvent(currentTeamDiscussionDetails: TeamDiscussionDetails): NewDiscussionDiscoveredEvent = {
        import currentTeamDiscussionDetails._
        NewDiscussionDiscoveredEvent(
          author = discussion.author,
          title = discussion.title,
          url = discussion.url,
          teamName = team.name,
          totalCommentsCount = discussion.commentsCount,
          addressees = discussion.addressees.map(refineV[AddresseeSpec](_).getOrElse(throw new IllegalStateException())),
          createdAt = discussion.createdAt)
      }

      maybePrevious.fold[Option[Event]](generateNewDiscussionDiscoveredEvent(current).some) { previous =>
        if (current.comments.size > previous.comments.size) {

          import current._
          val newCurrentComments = comments.drop(previous.comments.size)
            .map(c => NewComment(
              author = c.author,
              url = c.url,
              addressees = c.addressees.map(refineV[AddresseeSpec](_).getOrElse(throw new IllegalStateException())),
              createdAt = c.createdAt))

          NewCommentsDiscoveredEvent(
            teamName = team.name,
            title = discussion.title,
            totalCommentsCount = discussion.commentsCount,
            comments = NonEmptyList(newCurrentComments.head, newCurrentComments.tail)).some

        } else {
          none[Event]
        }
      }
    }
}


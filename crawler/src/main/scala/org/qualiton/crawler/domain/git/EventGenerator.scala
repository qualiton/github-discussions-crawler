package org.qualiton.crawler.domain.git

import _root_.cats.data.NonEmptyList
import _root_.cats.syntax.option._
import cats.effect.Sync

import eu.timepit.refined._
import org.qualiton.crawler.domain.core._

object EventGenerator {

  def generateEvent[F[_] : Sync](maybePrevious: Option[Discussion], current: Discussion): F[Option[Event]] =
    Sync[F].delay {

      def generateNewDiscussionDiscoveredEvent(currentDiscussion: Discussion): NewDiscussionDiscoveredEvent = {
        NewDiscussionDiscoveredEvent(
          author = currentDiscussion.author,
          title = currentDiscussion.title,
          url = currentDiscussion.url,
          teamName = currentDiscussion.teamName,
          totalCommentsCount = currentDiscussion.comments.size,
          addressees = currentDiscussion.addressees.map(refineV[AddresseeSpec](_).getOrElse(throw new IllegalStateException())),
          createdAt = currentDiscussion.createdAt)
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
            teamName = teamName,
            title = title,
            totalCommentsCount = comments.size,
            comments = NonEmptyList(newCurrentComments.head, newCurrentComments.tail)).some

        } else {
          none[Event]
        }
      }
    }
}


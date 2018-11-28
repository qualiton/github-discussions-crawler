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
          avatarUrl = currentDiscussion.avatarUrl,
          title = currentDiscussion.title,
          discussionUrl = currentDiscussion.discussionUrl,
          teamName = currentDiscussion.teamName,
          totalCommentsCount = currentDiscussion.comments.size,
          targeted = currentDiscussion.targeted.flatMap(refineV[TargetedSpec](_).toOption),
          createdAt = currentDiscussion.updatedAt)
      }

      maybePrevious.fold[Option[Event]](generateNewDiscussionDiscoveredEvent(current).some) { previous =>
        if (current.comments.size > previous.comments.size) {

          import current._
          val newCurrentComments = comments.drop(previous.comments.size)
            .map(c => NewComment(
              author = c.author,
              avatarUrl = c.avatarUrl,
              commentUrl = c.commentUrl,
              targeted = c.targeted.flatMap(refineV[TargetedSpec](_).toOption),
              updatedAt = c.updatedAt))

          NewCommentsDiscoveredEvent(
            teamName = teamName,
            title = title,
            totalCommentsCount = comments.size,
            newComments = NonEmptyList(newCurrentComments.head, newCurrentComments.tail),
            createdAt = newCurrentComments.map(_.updatedAt).max).some

        } else {
          none[Event]
        }
      }
    }
}


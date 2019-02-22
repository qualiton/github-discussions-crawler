package org.qualiton.crawler.domain.git

import _root_.cats.data.NonEmptyList
import _root_.cats.syntax.option._
import cats.effect.Sync

import eu.timepit.refined._

import org.qualiton.crawler.domain.core._

object EventGenerator {

  def generateEvent[F[_] : Sync](maybePrevious: Option[TeamDiscussionAggregateRoot], current: TeamDiscussionAggregateRoot): F[Option[DiscussionEvent]] =
    Sync[F].delay {

      def generateNewDiscussionDiscoveredEvent(teamDiscussionAggregateRoot: TeamDiscussionAggregateRoot): NewDiscussionDiscoveredEvent = {
        import teamDiscussionAggregateRoot._
        NewDiscussionDiscoveredEvent(
          authorName = initialComment.author.name,
          avatarUrl = initialComment.author.avatarUrl,
          title = discussion.title,
          discussionUrl = initialComment.url,
          teamName = team.name,
          totalCommentsCount = totalCommentsCount,
          targeted =
            Targeted(
              persons = initialComment.targetedPerson.flatMap(refineV[TargetedPersonSpec](_).toOption),
              teams = initialComment.targetedTeam.flatMap(refineV[TargetedTeamSpec](_).toOption)),
          createdAt = discussion.createdAt)
      }

      maybePrevious.fold[Option[DiscussionEvent]](generateNewDiscussionDiscoveredEvent(current).some) { previous =>
        if (current.totalCommentsCount > previous.totalCommentsCount) {

          import current._
          val newCurrentComments = discussion.comments.toList.drop(previous.discussion.comments.size)
            .map(c => NewComment(
              authorName = c.author.name,
              avatarUrl = c.author.avatarUrl,
              commentUrl = c.url,
              Targeted(
                persons = c.targetedPerson.flatMap(refineV[TargetedPersonSpec](_).toOption),
                teams = c.targetedTeam.flatMap(refineV[TargetedTeamSpec](_).toOption)),
              createdAt = c.createdAt))

          NewCommentsDiscoveredEvent(
            teamName = team.name,
            title = discussion.title,
            totalCommentsCount = totalCommentsCount,
            newComments = NonEmptyList(newCurrentComments.head, newCurrentComments.tail)).some

        } else {
          none[DiscussionEvent]
        }
      }
    }
}


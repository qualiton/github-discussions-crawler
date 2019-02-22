package org.qualiton.crawler.domain.git

import cats.data.NonEmptyList
import cats.effect.IO

import monocle.macros.syntax.lens._

import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.GenSupport
import org.qualiton.crawler.domain.core.{ DiscussionEvent, NewComment, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent }

class EventGeneratorSpec
  extends FreeSpecSupport
    with GenSupport {

  "EventGenerator" - {
    "should generate NewDiscussionDiscoveredEvent when a new discussion is discovered" in forAll { currentTeamDiscussionAggregateRoot: TeamDiscussionAggregateRoot =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](None, currentTeamDiscussionAggregateRoot).unsafeRunSync()

      inside(result) {
        case Some(NewDiscussionDiscoveredEvent(teamName, title, author, avatarUrl, discussionUrl, totalCommentsCount, targeted, createdAt)) =>
          teamName.value should ===(currentTeamDiscussionAggregateRoot.team.name.value)
          title.value should ===(currentTeamDiscussionAggregateRoot.discussion.title.value)
          author.value should ===(currentTeamDiscussionAggregateRoot.initialComment.author.name.value)
          avatarUrl.value should ===(currentTeamDiscussionAggregateRoot.initialComment.author.avatarUrl.value)
          discussionUrl.value should ===(currentTeamDiscussionAggregateRoot.initialComment.url.value)
          totalCommentsCount should ===(currentTeamDiscussionAggregateRoot.totalCommentsCount)
          targeted.persons should have size currentTeamDiscussionAggregateRoot.initialComment.targetedPerson.size.toLong
          targeted.teams should have size currentTeamDiscussionAggregateRoot.initialComment.targetedTeam.size.toLong
          createdAt should ===(currentTeamDiscussionAggregateRoot.discussion.createdAt)
      }
    }

    "should not generate event when there is no change in the discussion" in forAll { teamDiscussionAggregateRoot: TeamDiscussionAggregateRoot =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(teamDiscussionAggregateRoot), teamDiscussionAggregateRoot).unsafeRunSync()

      result shouldBe None
    }

    "should generate NewCommentsDiscoveredEvent when there are new comments for the discussion" in forAll { (previousTeamDiscussionAggregateRoot: TeamDiscussionAggregateRoot, extraComments: NonEmptyList[Comment]) =>

      val currentTeamDiscussionAggregateRoot = previousTeamDiscussionAggregateRoot.lens(_.discussion.comments).modify(_ ::: extraComments)
      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(previousTeamDiscussionAggregateRoot), currentTeamDiscussionAggregateRoot).unsafeRunSync()

      inside(result) {
        case Some(n@NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, _)) =>
          teamName.value should ===(currentTeamDiscussionAggregateRoot.team.name.value)
          title.value should ===(currentTeamDiscussionAggregateRoot.discussion.title.value)
          totalCommentsCount should ===(currentTeamDiscussionAggregateRoot.totalCommentsCount)
          n.createdAt should ===(extraComments.map(_.createdAt).toList.max)
      }
    }

    "should generate NewCommentsDiscoveredEvent when there is a new comment for the discussion" in forAll { (previousTeamDiscussionAggregateRoot: TeamDiscussionAggregateRoot, extraComment: Comment) =>

      val currentTeamDiscussionAggregateRoot = previousTeamDiscussionAggregateRoot.lens(_.discussion.comments).modify(_ :+ extraComment)
      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(previousTeamDiscussionAggregateRoot), currentTeamDiscussionAggregateRoot).unsafeRunSync()

      inside(result) {
        case Some(n@NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, NonEmptyList(NewComment(authorName, avatarUrl, commentUrl, targeted, createdAt), _))) =>
          teamName.value should ===(currentTeamDiscussionAggregateRoot.team.name.value)
          title.value should ===(currentTeamDiscussionAggregateRoot.discussion.title.value)
          totalCommentsCount should ===(currentTeamDiscussionAggregateRoot.totalCommentsCount)
          authorName.value should ===(extraComment.author.name.value)
          avatarUrl.value should ===(extraComment.author.avatarUrl.value)
          commentUrl.value should ===(extraComment.url.value)
          targeted.persons should have size extraComment.targetedPerson.size.toLong
          targeted.teams should have size extraComment.targetedTeam.size.toLong
          createdAt should ===(extraComment.createdAt)
          n.createdAt should ===(extraComment.createdAt)
      }
    }
  }

}

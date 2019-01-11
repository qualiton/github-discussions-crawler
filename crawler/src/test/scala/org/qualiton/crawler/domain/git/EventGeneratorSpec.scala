package org.qualiton.crawler.domain.git

import cats.data.NonEmptyList
import cats.effect.IO

import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.GenSupport
import org.qualiton.crawler.domain.core.{ DiscussionEvent, NewComment, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent }

class EventGeneratorSpec
  extends FreeSpecSupport
    with GenSupport {

  "EventGenerator" - {
    "should generate NewDiscussionDiscoveredEvent when a new discussion is discovered" in forAll { discussion: Discussion =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](None, discussion).unsafeRunSync()

      inside(result) {
        case Some(NewDiscussionDiscoveredEvent(teamName, title, author, avatarUrl, discussionUrl, totalCommentsCount, targeted, createdAt)) =>
          teamName.value should ===(discussion.teamName.value)
          title.value should ===(discussion.title.value)
          author.value should ===(discussion.author.value)
          avatarUrl.value should ===(discussion.avatarUrl.value)
          discussionUrl.value should ===(discussion.discussionUrl.value)
          totalCommentsCount should ===(discussion.comments.size)
          targeted.persons should have size discussion.targetedPerson.size.toLong
          targeted.teams should have size discussion.targetedTeam.size.toLong
          createdAt should ===(discussion.updatedAt)
      }
    }

    "should not generate event when there is no change in the discussion" in forAll { discussion: Discussion =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(discussion), discussion).unsafeRunSync()

      result shouldBe None
    }

    "should generate NewCommentsDiscoveredEvent when there are new comments for the discussion" in forAll { (discussion: Discussion, extraComments: NonEmptyList[Comment]) =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(discussion), discussion.copy(comments = discussion.comments ::: extraComments.toList)).unsafeRunSync()

      inside(result) {
        case Some(NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, newComments, createdAt)) =>
          teamName.value should ===(discussion.teamName.value)
          title.value should ===(discussion.title.value)
          totalCommentsCount should ===(discussion.comments.size + newComments.size)
          createdAt should ===(extraComments.map(_.updatedAt).toList.max)
      }
    }

    "should generate NewCommentsDiscoveredEvent when there is a new comments for the discussion" in forAll { (discussion: Discussion, extraComment: Comment) =>

      val result: Option[DiscussionEvent] = EventGenerator.generateEvent[IO](Some(discussion), discussion.copy(comments = discussion.comments ::: List(extraComment))).unsafeRunSync()

      inside(result) {
        case Some(NewCommentsDiscoveredEvent(teamName, title, totalCommentsCount, NonEmptyList(NewComment(author, avatarUrl, commentUrl, targeted, updatedAt), _), createdAt)) =>
          teamName.value should ===(discussion.teamName.value)
          title.value should ===(discussion.title.value)
          totalCommentsCount should ===(discussion.comments.size + 1)
          author.value should ===(extraComment.author.value)
          avatarUrl.value should ===(extraComment.avatarUrl.value)
          commentUrl.value should ===(extraComment.commentUrl.value)
          targeted.persons should have size extraComment.targetedPerson.size.toLong
          targeted.teams should have size extraComment.targetedTeam.size.toLong
          updatedAt should ===(extraComment.updatedAt)
          createdAt should ===(extraComment.updatedAt)
      }
    }
  }

}

package org.qualiton.crawler.infrastructure.rest.slack

import cats.data.NonEmptyList

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.W

import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.GenSupport
import org.qualiton.crawler.domain.core.{ NewComment, NewCommentsDiscoveredEvent, NewDiscussionDiscoveredEvent, Targeted, TargetedPerson, TargetedTeam }
import org.qualiton.slack.models.{ Attachment, ChatMessage, Field }

class ChatMessageAssemblerSpec
  extends FreeSpecSupport
    with GenSupport {

  "NewDiscussionDiscoveredEvent" - {
    "should be generated with zero comments and empty targets" in forAll { newDiscussionDiscoveredEvent: NewDiscussionDiscoveredEvent =>

      val newDiscussionDiscoveredEventWithZeroComments =
        newDiscussionDiscoveredEvent
          .copy(totalCommentsCount = 0, targeted = Targeted(Set(), Set()))

      val result: ChatMessage = ChatMessageAssembler.fromDomain(newDiscussionDiscoveredEventWithZeroComments)

      val expectedTeamName = newDiscussionDiscoveredEvent.teamName.value
      inside(result) {
        case ChatMessage(None, List(Attachment(pretext, color, author_name, author_icon, title, title_link, Field("Team", `expectedTeamName`, true) :: Nil, ts))) =>
          pretext should ===("New discussion has been discovered")
          color should ===("good")
          author_name should ===(newDiscussionDiscoveredEvent.authorName.value)
          author_icon should ===(newDiscussionDiscoveredEvent.avatarUrl.value)
          title should ===(newDiscussionDiscoveredEvent.title.value)
          title_link should ===(newDiscussionDiscoveredEvent.discussionUrl.value)
          ts should ===(newDiscussionDiscoveredEvent.createdAt.getEpochSecond)
      }
    }

    "should be generated with more than 1 comments and empty targets" in {
      forAll { (newDiscussionDiscoveredEvent: NewDiscussionDiscoveredEvent, totalCommentsCount: Int Refined Greater[W.`1`.T]) =>

        val newDiscussionDiscoveredEventWithZeroComments =
          newDiscussionDiscoveredEvent
            .copy(totalCommentsCount = totalCommentsCount.value, targeted = Targeted(Set(), Set()))

        val result: ChatMessage = ChatMessageAssembler.fromDomain(newDiscussionDiscoveredEventWithZeroComments)

        val expectedTeamName = newDiscussionDiscoveredEvent.teamName.value
        val expectedTotalCommentsCount = totalCommentsCount.value.toString
        inside(result) {
          case ChatMessage(
          None, List(Attachment(
          pretext, color, author_name, author_icon, title, title_link,
          Field("Team", `expectedTeamName`, true) :: Field("Comments", `expectedTotalCommentsCount`, true) :: Nil, ts))) =>

            pretext should ===(s"New discussion has been discovered with ${ totalCommentsCount.value } comments")
            color should ===("good")
            author_name should ===(newDiscussionDiscoveredEvent.authorName.value)
            author_icon should ===(newDiscussionDiscoveredEvent.avatarUrl.value)
            title should ===(newDiscussionDiscoveredEvent.title.value)
            title_link should ===(newDiscussionDiscoveredEvent.discussionUrl.value)
            ts should ===(newDiscussionDiscoveredEvent.createdAt.getEpochSecond)
        }
      }
    }

    "should be generated with more than 1 comments and more targets" in {
      forAll { (newDiscussionDiscoveredEvent: NewDiscussionDiscoveredEvent, totalCommentsCount: Int Refined Greater[W.`1`.T], targetedPersons: NonEmptyList[TargetedPerson], targetedTeams: NonEmptyList[TargetedTeam]) =>

        val newDiscussionDiscoveredEventWithComments =
          newDiscussionDiscoveredEvent
            .copy(totalCommentsCount = totalCommentsCount.value, targeted = Targeted(targetedPersons.toList.toSet, targetedTeams.toList.toSet))

        val result: ChatMessage = ChatMessageAssembler.fromDomain(newDiscussionDiscoveredEventWithComments)

        val expectedTeamName = newDiscussionDiscoveredEvent.teamName.value
        val expectedTotalCommentsCount = totalCommentsCount.value.toString
        inside(result) {
          case ChatMessage(
          None, List(Attachment(
          pretext, color, author_name, author_icon, title, title_link,
          Field("Team", `expectedTeamName`, true) :: Field("Comments", `expectedTotalCommentsCount`, true) :: Field("Targeted", targeted, false) :: Nil, ts))) =>

            pretext should ===(s"New discussion has been discovered with ${ totalCommentsCount.value } comments")
            color should ===("good")
            author_name should ===(newDiscussionDiscoveredEvent.authorName.value)
            author_icon should ===(newDiscussionDiscoveredEvent.avatarUrl.value)
            title should ===(newDiscussionDiscoveredEvent.title.value)
            title_link should ===(newDiscussionDiscoveredEvent.discussionUrl.value)
            targeted should ===((targetedPersons.map(_.value).toList.sorted ::: targetedTeams.map(_.value).toList.sorted).distinct.mkString(", "))
            ts should ===(newDiscussionDiscoveredEvent.createdAt.getEpochSecond)
        }
      }
    }
  }

    "NewCommentsDiscoveredEvent" - {
      "should be generated with 1 new comment and empty targets" in forAll { (newCommentsDiscoveredEvent: NewCommentsDiscoveredEvent, comment: NewComment) =>

        val newCommentsDiscoveredEventWithComments =
          newCommentsDiscoveredEvent
            .copy(newComments = NonEmptyList.one(comment.copy(targeted = Targeted(Set(), Set()))))

        val result: ChatMessage = ChatMessageAssembler.fromDomain(newCommentsDiscoveredEventWithComments)

        val expectedTeamName = newCommentsDiscoveredEventWithComments.teamName.value
        val expectedTotalCommentsCount = newCommentsDiscoveredEventWithComments.totalCommentsCount.toString
        inside(result) {
          case ChatMessage(None, List(Attachment(pretext, color, author_name, author_icon, title, title_link, Field("Team", `expectedTeamName`, true) :: Field("Comments", `expectedTotalCommentsCount`, true) :: Nil, ts))) =>
            pretext should ===("New comment has been discovered")
            color should ===("good")
            author_name should ===(comment.authorName.value)
            author_icon should ===(comment.avatarUrl.value)
            title should ===(newCommentsDiscoveredEvent.title.value)
            title_link should ===(comment.commentUrl.value)
            ts should ===(comment.createdAt.getEpochSecond)
        }
      }

//      "should be generated with more than 1 new comment and empty targets" in {
//        forAll { (newCommentsDiscoveredEvent: NewCommentsDiscoveredEvent, comment: NewComment, totalCommentsCount: Int Refined Greater[W.`0`.T]) =>
//
//          val newModifiedCommment = comment.copy(targeted = Targeted(Set(), Set()))
//          val newCommentsDiscoveredEventWithComments =
//            newCommentsDiscoveredEvent
//              .copy(newComments = NonEmptyList.of(newModifiedCommment, List.fill(totalCommentsCount.value)(newModifiedCommment): _*))
//
//          val result: ChatMessage = ChatMessageAssembler.fromDomain(newCommentsDiscoveredEventWithComments)
//
//          val expectedTeamName = newCommentsDiscoveredEventWithComments.teamName.value
//          val expectedTotalCommentsCount = newCommentsDiscoveredEventWithComments.totalCommentsCount.toString
//          inside(result) {
//            case ChatMessage(None, List(Attachment(pretext, color, author_name, author_icon, title, title_link, Field("Team", `expectedTeamName`, true) :: Field("Comments", `expectedTotalCommentsCount`, true) :: Nil, ts))) =>
//              pretext should ===(s"${ newCommentsDiscoveredEventWithComments.newComments.size } new comments have been discovered")
//              color should ===("good")
//              author_name should ===(comment.authorName.value)
//              author_icon should ===(comment.avatarUrl.value)
//              title should ===(newCommentsDiscoveredEvent.title.value)
//              title_link should ===(comment.commentUrl.value)
//              ts should ===(comment.createdAt.getEpochSecond)
//          }
//        }
//      }
    }
}

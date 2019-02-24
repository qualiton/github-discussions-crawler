package org.qualiton.crawler.infrastructure.persistence.git

import cats.data.NonEmptyList
import cats.effect.IO

import org.qualiton.crawler.TestFixture
import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.domain.git.TeamDiscussionAggregateRoot
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.DiscussionAggregateRootPersistence

class GithubPostgresAssemblerSpec extends FreeSpecSupport
  with TestFixture {

  "GithubPostgresAssembler" - {
    "fromDomain" - {
      "should be able to generate the persistence aggregate with empty comments" in {
        val result = GithubPostgresAssembler.fromDomain(teamDiscussionAggregateRoot)

        inside(result) {
          case DiscussionAggregateRootPersistence(
          team,
          discussion,
          NonEmptyList(initialDiscussion, _)) =>
            team.id should ===(teamDiscussionAggregateRoot.team.id.value)
            team.name should ===(teamDiscussionAggregateRoot.team.name.value)
            team.description should ===(teamDiscussionAggregateRoot.team.description)
            team.createdAt should ===(teamDiscussionAggregateRoot.team.createdAt)
            team.updatedAt should ===(teamDiscussionAggregateRoot.team.updatedAt)

            discussion.teamId should ===(teamDiscussionAggregateRoot.team.id.value)
            discussion.discussionId should ===(teamDiscussionAggregateRoot.discussion.id.value)
            discussion.title should ===(teamDiscussionAggregateRoot.discussion.title.value)
            discussion.createdAt should ===(teamDiscussionAggregateRoot.discussion.createdAt)
            discussion.updatedAt should ===(teamDiscussionAggregateRoot.discussion.updatedAt)

            initialDiscussion.teamId should ===(teamDiscussionAggregateRoot.team.id.value)
            initialDiscussion.discussionId should ===(teamDiscussionAggregateRoot.discussion.id.value)
            initialDiscussion.commentId should ===(teamDiscussionAggregateRoot.initialComment.id.value)
            initialDiscussion.author.id should ===(teamDiscussionAggregateRoot.initialComment.author.id.value)
            initialDiscussion.author.name should ===(teamDiscussionAggregateRoot.initialComment.author.name.value)
            initialDiscussion.author.avatarUrl should ===(teamDiscussionAggregateRoot.initialComment.author.avatarUrl.value)
            initialDiscussion.url should ===(teamDiscussionAggregateRoot.initialComment.url.value)
            initialDiscussion.body should ===(teamDiscussionAggregateRoot.initialComment.body.value)
            initialDiscussion.bodyVersion should ===(teamDiscussionAggregateRoot.initialComment.bodyVersion.value)
            initialDiscussion.createdAt should ===(teamDiscussionAggregateRoot.initialComment.createdAt)
            initialDiscussion.updatedAt should ===(teamDiscussionAggregateRoot.initialComment.updatedAt)
        }
      }
    }

    "toDomain" - {
      "should be able to generate the aggregate with empty comments" in {
        val result = GithubPostgresAssembler.toDomain[IO](discussionAggregateRootPersistence).unsafeRunSync()

        inside(result) {
          case t@TeamDiscussionAggregateRoot(team, discussion) =>
            team.id.value should ===(userTeamResponse.id)
            team.name.value should ===(userTeamResponse.name)
            team.description should ===(userTeamResponse.description)
            team.createdAt should ===(userTeamResponse.created_at)
            team.updatedAt should ===(userTeamResponse.updated_at)

            discussion.id.value should ===(teamDiscussionResponse.number)
            discussion.title.value should ===(teamDiscussionResponse.title)
            discussion.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.updatedAt should ===(teamDiscussionResponse.updated_at)

            t.initialComment.id.value should ===(commentPersistence0.commentId)
            t.initialComment.url.value should ===(commentPersistence0.url)
            t.initialComment.author.id.value should ===(commentPersistence0.author.id)
            t.initialComment.author.name.value should ===(commentPersistence0.author.name)
            t.initialComment.author.avatarUrl.value should ===(commentPersistence0.author.avatarUrl)
            t.initialComment.body.value should ===(commentPersistence0.body)
            t.initialComment.bodyVersion.value should ===(commentPersistence0.bodyVersion)
            t.initialComment.createdAt should ===(commentPersistence0.createdAt)
            t.initialComment.updatedAt should ===(commentPersistence0.updatedAt)
        }
      }

      "should be able to generate the aggregate with 1 comment" in {

        val discussionAggregateRootPersistenceWithOneComment =
          discussionAggregateRootPersistence.copy(comments = NonEmptyList.of(commentPersistence0, commentPersistence1))
        val result = GithubPostgresAssembler.toDomain[IO](discussionAggregateRootPersistenceWithOneComment).unsafeRunSync()

        inside(result) {
          case t@TeamDiscussionAggregateRoot(team, discussion) =>
            team.id.value should ===(userTeamResponse.id)
            team.name.value should ===(userTeamResponse.name)
            team.description should ===(userTeamResponse.description)
            team.createdAt should ===(userTeamResponse.created_at)
            team.updatedAt should ===(userTeamResponse.updated_at)

            discussion.id.value should ===(teamDiscussionResponse.number)
            discussion.title.value should ===(teamDiscussionResponse.title)
            discussion.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.updatedAt should ===(teamDiscussionResponse.updated_at)

            t.initialComment.id.value should ===(commentPersistence0.commentId)
            t.initialComment.url.value should ===(commentPersistence0.url)
            t.initialComment.author.id.value should ===(commentPersistence0.author.id)
            t.initialComment.author.name.value should ===(commentPersistence0.author.name)
            t.initialComment.author.avatarUrl.value should ===(commentPersistence0.author.avatarUrl)
            t.initialComment.body.value should ===(commentPersistence0.body)
            t.initialComment.bodyVersion.value should ===(commentPersistence0.bodyVersion)
            t.initialComment.createdAt should ===(commentPersistence0.createdAt)
            t.initialComment.updatedAt should ===(commentPersistence0.updatedAt)

            discussion.comments.last.id.value should ===(commentPersistence1.commentId)
            discussion.comments.last.url.value should ===(commentPersistence1.url)
            discussion.comments.last.author.id.value should ===(commentPersistence1.author.id)
            discussion.comments.last.author.name.value should ===(commentPersistence1.author.name)
            discussion.comments.last.author.avatarUrl.value should ===(commentPersistence1.author.avatarUrl)
            discussion.comments.last.body.value should ===(commentPersistence1.body)
            discussion.comments.last.bodyVersion.value should ===(commentPersistence1.bodyVersion)
            discussion.comments.last.createdAt should ===(commentPersistence1.createdAt)
            discussion.comments.last.updatedAt should ===(commentPersistence1.updatedAt)
        }
      }
    }
  }
}


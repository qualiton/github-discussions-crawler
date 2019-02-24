package org.qualiton.crawler.infrastructure.rest.git

import cats.effect.IO

import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.TestFixture
import org.qualiton.crawler.domain.git.TeamDiscussionAggregateRoot

class DiscussionRestAssemblerSpec
  extends FreeSpecSupport
    with TestFixture {

  "DiscussionRestAssembler" - {
    "toDomain" - {
      "should be able to generate the aggregate with empty comments" in {
        val result = DiscussionRestAssembler.toDomain[IO](userTeamResponse, teamDiscussionResponse, List.empty).unsafeRunSync()

        inside(result) {
          case TeamDiscussionAggregateRoot(team, discussion) =>
            team.id.value should ===(userTeamResponse.id)
            team.name.value should ===(userTeamResponse.name)
            team.description should ===(userTeamResponse.description)
            team.createdAt should ===(userTeamResponse.created_at)
            team.updatedAt should ===(userTeamResponse.updated_at)

            discussion.id.value should ===(teamDiscussionResponse.number)
            discussion.title.value should ===(teamDiscussionResponse.title)
            discussion.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.updatedAt should ===(teamDiscussionResponse.updated_at)

            discussion.comments.head.id.value should ===(0L)
            discussion.comments.head.url.value should ===(teamDiscussionResponse.html_url)
            discussion.comments.head.author.id.value should ===(teamDiscussionResponse.author.id)
            discussion.comments.head.author.name.value should ===(teamDiscussionResponse.author.login)
            discussion.comments.head.author.avatarUrl.value should ===(teamDiscussionResponse.author.avatar_url)
            discussion.comments.head.body.value should ===(teamDiscussionResponse.body)
            discussion.comments.head.bodyVersion.value should ===(teamDiscussionResponse.body_version)
            discussion.comments.head.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.comments.head.updatedAt should ===(teamDiscussionResponse.updated_at)
        }
      }

      "should be able to generate the aggregate with 1 comment" in {
        val result = DiscussionRestAssembler.toDomain[IO](userTeamResponse, teamDiscussionResponse, List(teamDiscussionCommentEntity)).unsafeRunSync()

        inside(result) {
          case TeamDiscussionAggregateRoot(team, discussion) =>
            team.id.value should ===(userTeamResponse.id)
            team.name.value should ===(userTeamResponse.name)
            team.description should ===(userTeamResponse.description)
            team.createdAt should ===(userTeamResponse.created_at)
            team.updatedAt should ===(userTeamResponse.updated_at)

            discussion.id.value should ===(teamDiscussionResponse.number)
            discussion.title.value should ===(teamDiscussionResponse.title)
            discussion.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.updatedAt should ===(teamDiscussionResponse.updated_at)

            discussion.comments.head.id.value should ===(0L)
            discussion.comments.head.url.value should ===(teamDiscussionResponse.html_url)
            discussion.comments.head.author.id.value should ===(teamDiscussionResponse.author.id)
            discussion.comments.head.author.name.value should ===(teamDiscussionResponse.author.login)
            discussion.comments.head.author.avatarUrl.value should ===(teamDiscussionResponse.author.avatar_url)
            discussion.comments.head.body.value should ===(teamDiscussionResponse.body)
            discussion.comments.head.bodyVersion.value should ===(teamDiscussionResponse.body_version)
            discussion.comments.head.createdAt should ===(teamDiscussionResponse.created_at)
            discussion.comments.head.updatedAt should ===(teamDiscussionResponse.updated_at)

            discussion.comments.last.id.value should ===(teamDiscussionCommentEntity.number)
            discussion.comments.last.url.value should ===(teamDiscussionCommentEntity.html_url)
            discussion.comments.last.author.id.value should ===(teamDiscussionCommentEntity.author.id)
            discussion.comments.last.author.name.value should ===(teamDiscussionCommentEntity.author.login)
            discussion.comments.last.author.avatarUrl.value should ===(teamDiscussionCommentEntity.author.avatar_url)
            discussion.comments.last.body.value should ===(teamDiscussionCommentEntity.body)
            discussion.comments.last.bodyVersion.value should ===(teamDiscussionCommentEntity.body_version)
            discussion.comments.last.createdAt should ===(teamDiscussionCommentEntity.created_at)
            discussion.comments.last.updatedAt should ===(teamDiscussionCommentEntity.updated_at)
        }
      }
    }
  }
}

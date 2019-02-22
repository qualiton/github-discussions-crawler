package org.qualiton.crawler.infrastructure.rest.git

import cats.effect.IO

import org.qualiton.crawler.common.testsupport.FreeSpecSupport
import org.qualiton.crawler.TestFixture
import org.qualiton.crawler.domain.git.TeamDiscussionAggregateRoot

class DiscussionRestAssemblerSpec
  extends FreeSpecSupport
    with TestFixture {

  "DiscussionRestAssembler" - {
    "should be able to generate the aggregate with empty comments" in {
      val result = DiscussionRestAssembler.toDomain[IO](userTeamResponse, teamDiscussionResponse, List.empty).unsafeRunSync()

      inside(result) {
        case TeamDiscussionAggregateRoot(team, description) =>
          team.id.value should ===(userTeamResponse.id)
          team.name.value should ===(userTeamResponse.name)
          team.description should ===(userTeamResponse.description)
          team.createdAt should ===(userTeamResponse.created_at)
          team.updatedAt should ===(userTeamResponse.updated_at)

          description.id.value should ===(teamDiscussionResponse.number)
          description.title.value should ===(teamDiscussionResponse.title)
          description.createdAt should ===(teamDiscussionResponse.created_at)
          description.updatedAt should ===(teamDiscussionResponse.updated_at)

          description.comments.head.id.value should ===(0L)
          description.comments.head.url.value should ===(teamDiscussionResponse.html_url)
          description.comments.head.author.id.value should ===(teamDiscussionResponse.author.id)
          description.comments.head.author.name.value should ===(teamDiscussionResponse.author.login)
          description.comments.head.author.avatarUrl.value should ===(teamDiscussionResponse.author.avatar_url)
          description.comments.head.body.value should ===(teamDiscussionResponse.body)
          description.comments.head.bodyVersion.value should ===(teamDiscussionResponse.body_version)
          description.comments.head.createdAt should ===(teamDiscussionResponse.created_at)
          description.comments.head.updatedAt should ===(teamDiscussionResponse.updated_at)
      }
    }

    "should be able to generate the aggregate with 1 comment" in {
      val result = DiscussionRestAssembler.toDomain[IO](userTeamResponse, teamDiscussionResponse, List(teamDiscussionCommentEntity)).unsafeRunSync()

      inside(result) {
        case TeamDiscussionAggregateRoot(team, description) =>
          team.id.value should ===(userTeamResponse.id)
          team.name.value should ===(userTeamResponse.name)
          team.description should ===(userTeamResponse.description)
          team.createdAt should ===(userTeamResponse.created_at)
          team.updatedAt should ===(userTeamResponse.updated_at)

          description.id.value should ===(teamDiscussionResponse.number)
          description.title.value should ===(teamDiscussionResponse.title)
          description.createdAt should ===(teamDiscussionResponse.created_at)
          description.updatedAt should ===(teamDiscussionResponse.updated_at)

          description.comments.head.id.value should ===(0L)
          description.comments.head.url.value should ===(teamDiscussionResponse.html_url)
          description.comments.head.author.id.value should ===(teamDiscussionResponse.author.id)
          description.comments.head.author.name.value should ===(teamDiscussionResponse.author.login)
          description.comments.head.author.avatarUrl.value should ===(teamDiscussionResponse.author.avatar_url)
          description.comments.head.body.value should ===(teamDiscussionResponse.body)
          description.comments.head.bodyVersion.value should ===(teamDiscussionResponse.body_version)
          description.comments.head.createdAt should ===(teamDiscussionResponse.created_at)
          description.comments.head.updatedAt should ===(teamDiscussionResponse.updated_at)

          description.comments.last.id.value should ===(teamDiscussionCommentEntity.number)
          description.comments.last.url.value should ===(teamDiscussionCommentEntity.html_url)
          description.comments.last.author.id.value should ===(teamDiscussionCommentEntity.author.id)
          description.comments.last.author.name.value should ===(teamDiscussionCommentEntity.author.login)
          description.comments.last.author.avatarUrl.value should ===(teamDiscussionCommentEntity.author.avatar_url)
          description.comments.last.body.value should ===(teamDiscussionCommentEntity.body)
          description.comments.last.bodyVersion.value should ===(teamDiscussionCommentEntity.body_version)
          description.comments.last.createdAt should ===(teamDiscussionCommentEntity.created_at)
          description.comments.last.updatedAt should ===(teamDiscussionCommentEntity.updated_at)
      }
    }
  }
}

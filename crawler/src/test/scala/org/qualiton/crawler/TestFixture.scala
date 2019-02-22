package org.qualiton.crawler

import java.time.Instant

import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient.{ AuthorEntity, TeamDiscussionCommentEntity, TeamDiscussionResponse, UserTeamResponse }

trait TestFixture {

  val userTeamResponse: UserTeamResponse =
    UserTeamResponse(1, "name", "description", Instant.parse("2018-10-10T06:20:00Z"), Instant.parse("2018-10-10T08:45:00Z"))
  val teamDiscussionResponse: TeamDiscussionResponse =
    TeamDiscussionResponse("title", 20L, AuthorEntity(40L, "author", "http://avatar.com"), "body", "bodyVersion", "http://discussion.com", Instant.parse("2018-10-10T06:45:00Z"), Instant.parse("2018-10-10T08:40:00Z"))
  val teamDiscussionCommentEntity: TeamDiscussionCommentEntity =
    TeamDiscussionCommentEntity(AuthorEntity(50L, "author2", "http://avatar2.com"), 1, "body2", "bodyVersion2", "http://discussion2.com", Instant.parse("2018-10-11T10:27:00Z"), Instant.parse("2018-10-11T10:56:00Z"))

}

object TestFixture extends TestFixture


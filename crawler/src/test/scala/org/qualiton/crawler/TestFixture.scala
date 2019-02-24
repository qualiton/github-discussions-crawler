package org.qualiton.crawler

import java.time.Instant

import cats.data.NonEmptyList

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{ Url => RefinedUrl }

import org.qualiton.crawler.domain.git.{ Author, Comment, Discussion, Team, TeamDiscussionAggregateRoot }
import org.qualiton.crawler.infrastructure.persistence.git.GithubPostgresRepository.{ AuthorPersistence, CommentPersistence, DiscussionAggregateRootPersistence, DiscussionPersistence, TeamPersistence }
import org.qualiton.crawler.infrastructure.rest.git.GithubHttp4sApiClient.{ AuthorEntity, TeamDiscussionCommentEntity, TeamDiscussionResponse, UserTeamResponse }

trait TestFixture {

  val userTeamResponse: UserTeamResponse =
    UserTeamResponse(1, "name", "description", Instant.parse("2018-10-10T06:20:00Z"), Instant.parse("2018-10-10T08:45:00Z"))
  val teamDiscussionResponse: TeamDiscussionResponse =
    TeamDiscussionResponse("title", 20L, AuthorEntity(40L, "author", "http://avatar.com"), "body", "bodyVersion", "http://discussion.com", Instant.parse("2018-10-10T06:45:00Z"), Instant.parse("2018-10-10T08:40:00Z"))
  val teamDiscussionCommentEntity: TeamDiscussionCommentEntity =
    TeamDiscussionCommentEntity(AuthorEntity(50L, "author2", "http://avatar2.com"), 1, "body2", "bodyVersion2", "http://discussion2.com", Instant.parse("2018-10-11T10:27:00Z"), Instant.parse("2018-10-11T10:56:00Z"))

  val team: Team =
    Team(
      refineV[NonNegative](1L).getOrElse(throw new IllegalArgumentException),
      refineV[NonEmpty]("name").getOrElse(throw new IllegalArgumentException),
      "description",
      Instant.parse("2018-10-10T06:20:00Z"),
      Instant.parse("2018-10-10T08:45:00Z"))


  val comment0: Comment =
    Comment(
      refineV[NonNegative](0L).getOrElse(throw new IllegalArgumentException),
      refineV[RefinedUrl]("http://comment0.com").getOrElse(throw new IllegalArgumentException),
      Author(
        refineV[NonNegative](50L).getOrElse(throw new IllegalArgumentException),
        refineV[NonEmpty]("author0").getOrElse(throw new IllegalArgumentException),
        refineV[RefinedUrl]("http://avatar0.com").getOrElse(throw new IllegalArgumentException),
      ),
      refineV[NonEmpty]("body0").getOrElse(throw new IllegalArgumentException),
      refineV[NonEmpty]("bodyVersion0").getOrElse(throw new IllegalArgumentException),
      Instant.parse("2018-10-12T10:27:00Z"),
      Instant.parse("2018-10-12T10:56:00Z"))

  val discussion: Discussion =
    Discussion(
      refineV[NonNegative](20L).getOrElse(throw new IllegalArgumentException),
      refineV[NonEmpty]("title").getOrElse(throw new IllegalArgumentException),
      NonEmptyList.of(comment0),
      Instant.parse("2018-10-10T06:45:00Z"),
      Instant.parse("2018-10-10T08:40:00Z"))

  val teamDiscussionAggregateRoot: TeamDiscussionAggregateRoot =
    TeamDiscussionAggregateRoot(team, discussion)

  val teamPersistence: TeamPersistence =
    TeamPersistence(1L, "name", "description", Instant.parse("2018-10-10T06:20:00Z"), Instant.parse("2018-10-10T08:45:00Z"))
  val discussionPersistence: DiscussionPersistence =
    DiscussionPersistence(1L, 20L, "title", Instant.parse("2018-10-10T06:45:00Z"), Instant.parse("2018-10-10T08:40:00Z"))
  val commentPersistence0: CommentPersistence =
    CommentPersistence(1L, 20L, 0L, AuthorPersistence(50L, "author0", "http://avatar0.com"), "http://comment0.com", "body0", "bodyVersion0", Instant.parse("2018-10-12T10:27:00Z"), Instant.parse("2018-10-12T10:56:00Z"))
  val commentPersistence1: CommentPersistence =
    CommentPersistence(1L, 20L, 1L, AuthorPersistence(51L, "author1", "http://avatar1.com"), "http://comment1.com", "body1", "bodyVersion1", Instant.parse("2018-10-13T10:27:00Z"), Instant.parse("2018-10-13T10:56:00Z"))
  val commentPersistence2: CommentPersistence =
    CommentPersistence(1L, 20L, 2L, AuthorPersistence(52L, "author2", "http://avatar2.com"), "http://comment2.com", "body2", "bodyVersion2", Instant.parse("2018-10-14T10:27:00Z"), Instant.parse("2018-10-14T10:56:00Z"))

  val discussionAggregateRootPersistence: DiscussionAggregateRootPersistence =
    DiscussionAggregateRootPersistence(teamPersistence, discussionPersistence, NonEmptyList.of(commentPersistence0))

}

object TestFixture extends TestFixture


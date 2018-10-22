package org.qualiton.crawler.infrastructure.http.git

import cats.data.Validated
import org.qualiton.crawler.domain.git.TeamDiscussionDetails
import org.qualiton.crawler.infrastructure.http.git.GithubHttp4sClient.{TeamDiscussionCommentResponse, TeamDiscussionResponse, UserTeamResponse}

object TeamDiscussionDetailsAssembler {

  def toDomain(userTeamResponse: UserTeamResponse,
               teamDiscussionResponse: TeamDiscussionResponse,
               teamDiscussionCommentResponse: List[TeamDiscussionCommentResponse]): Validated[Throwable, TeamDiscussionDetails] = ???
}

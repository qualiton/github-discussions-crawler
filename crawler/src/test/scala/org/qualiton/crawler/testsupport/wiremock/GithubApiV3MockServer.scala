package org.qualiton.crawler.testsupport.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.HttpStatus
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import org.qualiton.crawler.testsupport.resource.ResourceSupport
import org.qualiton.crawler.testsupport.wiremock.GithubApiV3MockServer._

class GithubApiV3MockServer(githubApiV3Port: Int = GithubApiV3Port) extends ResourceSupport {

  val server = new WireMockServer(wireMockConfig().port(githubApiV3Port))

  def startMockServer(): Unit = {
    server.start()
    configureFor(GithubApiV3Host, githubApiV3Port)
  }

  def stopMockServer(): Unit = {
    server.stop()
  }

  private def errorResponse: String =
    s"""
       |{
       |    "message": "Not Found",
       |    "documentation_url": "https://developer.github.com/v3/users/#get-a-single-user"
       |}
    """.stripMargin

  private def prepareResponse(resource: String, httpStatus: Int): String = {
    if (HttpStatus.isSuccess(httpStatus)) {
      loadResource(resource)
    } else {
      errorResponse
    }
  }

  def mockUserTeams(teamId: Long, httpStatus: Int, resource: String = "/userteams.json"): StubMapping =
    server.stubFor(get(urlPathMatching("/user/teams"))
      .withHeader("Authorization", equalTo(s"Basic $testEncodedApiToken"))
      .willReturn(aResponse()
        .withStatus(httpStatus)
        .withHeader("Content-type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, httpStatus)
          .replaceAll("TEAM_ID", teamId.toString))))

  def mockTeamDiscussions(teamId: Long, httpStatus: Int, resource: String = "/teamdiscussions-1.json"): StubMapping =
    server.stubFor(get(urlPathMatching(s"/teams/$teamId/discussions"))
      .withHeader("Authorization", equalTo(s"Basic $testEncodedApiToken"))
      .willReturn(aResponse()
        .withStatus(httpStatus)
        .withHeader("Content-type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, httpStatus)
          .replaceAll("TEAM_ID", teamId.toString))))

  def mockDiscussionComments(teamId: Long, discussionId: Long, httpStatus: Int, resource: String = "/discussioncomments-1.json"): StubMapping =
    server.stubFor(get(urlPathMatching(s"/teams/$teamId/discussions/$discussionId/comments"))
      .withHeader("Authorization", equalTo(s"Basic $testEncodedApiToken"))
      .willReturn(aResponse()
        .withStatus(httpStatus)
        .withHeader("Content-type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, httpStatus)
          .replaceAll("TEAM_ID", teamId.toString)
          .replaceAll("DISCUSSION_ID", discussionId.toString))))
}

object GithubApiV3MockServer {
  val GithubApiV3Host: String = "localhost"
  val GithubApiV3Port: Int = 3000

  val testApiToken: String = "testApiToken"
  val testEncodedApiToken: String = "dGVzdEFwaVRva2Vu"
}

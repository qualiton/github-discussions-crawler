package org.qualiton.slack.testsupport.wiremock

import scala.collection.JavaConverters._

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import org.apache.http.HttpStatus

import org.qualiton.slack.testsupport.resource.ResourceSupport
import org.qualiton.slack.testsupport.wiremock.SlackApiMockServer.{ testApiToken, SlackApiHost, SlackApiPort }
import org.qualiton.slack.testsupport.wiremock.SlackRtmApiMockServer.{ SlackRtmApiHost, SlackRtmApiPort }

class SlackApiMockServer(slackApiPort: Int = SlackApiPort) extends ResourceSupport {

  val server = new WireMockServer(wireMockConfig().port(slackApiPort))
  val wireMock = new WireMock(SlackApiHost, slackApiPort)

  def startMockServer(): Unit = {
    server.start()
  }

  def stopMockServer(): Unit = {
    server.stop()
  }

  private def errorResponse(error: String): String =
    s"""
       |{
       |  "ok": false,
       |  "error": "$error"
       |}
    """.stripMargin

  private def prepareResponse(resource: String, error: Option[String]): String =
    error.fold(loadResource(resource)) { error =>
      errorResponse(error)
    }

  /** *************************/
  /** *  Channel Endpoints  ***/
  /** *************************/

  def mockConversationsList(
      channelName1: String,
      channelName2: String = "CHANNEL_NAME2",
      error: Option[String] = None,
      resource: String = "/conversations.list.json"): StubMapping =
    wireMock.register(get(urlPathEqualTo("/api/conversations.list"))
      .withHeader("Authorization", equalTo(s"Bearer ${ testApiToken.value }"))
      .withQueryParams(Map(
        "exclude_archived" -> equalTo("true"),
        "types" -> equalTo("public_channel,private_channel"),
        "limit" -> equalTo("100")).asJava)
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader("Content-Type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, error)
          .replaceAll("CHANNEL_NAME1", channelName1)
          .replaceAll("CHANNEL_NAME2", channelName2))))

  /** ************************/
  /** **  Chat Endpoints  ****/
  /** ************************/

  def mockChatPostMessage(error: Option[String] = None, resource: String = "/chat.postMessage.json"): StubMapping =
    wireMock.register(post(urlPathEqualTo("/api/chat.postMessage"))
      .withHeader("Authorization", equalTo(s"Bearer ${ testApiToken.value }"))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader("Content-Type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, error))))

  /** ***********************/
  /** **  RTM Endpoints  ****/
  /** ***********************/

  def mockRtmConnect(error: Option[String] = None, resource: String = "/rtm.connect.json"): StubMapping =
    wireMock.register(get(urlPathEqualTo("/api/rtm.connect"))
      .withHeader("Authorization", equalTo(s"Bearer ${ testApiToken.value }"))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader("Content-Type", "application/json; charset=UTF-8")
        .withBody(prepareResponse(resource, error)
          .replaceAll("URL", s"ws://$SlackRtmApiHost:$SlackRtmApiPort/rtm"))))

  def findLastGetRequestFor(path: String): LoggedRequest =
    wireMock.find(getRequestedFor(urlEqualTo(path))).asScala.last

  def findLastPostRequestFor(path: String): LoggedRequest =
    wireMock.find(postRequestedFor(urlEqualTo(path))).asScala.last

  def resetRequests(): Unit =
    wireMock.resetRequests()
}

object SlackApiMockServer {

  val SlackApiHost: String = "localhost"
  val SlackApiPort: Int = 3001

  val testApiToken: NonEmptyString = "testApiToken"
}





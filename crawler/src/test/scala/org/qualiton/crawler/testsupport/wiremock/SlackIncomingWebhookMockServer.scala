package org.qualiton.crawler.testsupport.wiremock

import scala.collection.JavaConverters._

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import org.apache.http.HttpStatus

import org.qualiton.crawler.testsupport.wiremock.SlackIncomingWebhookMockServer._

class SlackIncomingWebhookMockServer(slackIncomingWebhookPort: Int = SlackIncomingWebhookPort) {

  val server = new WireMockServer(wireMockConfig().port(slackIncomingWebhookPort))
  val wireMock = new WireMock(SlackIncomingWebhookHost, slackIncomingWebhookPort)

  def startMockServer(): Unit = {
    server.start()
    mockIncomingWebhook()
    ()
  }

  def stopMockServer(): Unit = {
    server.stop()
  }

  def mockIncomingWebhook(httpStatus: Int = HttpStatus.SC_OK): StubMapping =
    wireMock.register(post(urlEqualTo(s"/$testApiToken"))
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(aResponse().withStatus(httpStatus)))

  def incomingWebhookCallRequest(): LoggedRequest =
    wireMock.find(postRequestedFor(urlEqualTo(s"/$testApiToken"))).asScala.last

  def resetRequests(): Unit =
    wireMock.resetRequests()
}

object SlackIncomingWebhookMockServer {
  val SlackIncomingWebhookHost: String = "localhost"
  val SlackIncomingWebhookPort: Int = 3001

  val testApiToken: String = "testApiToken"
}



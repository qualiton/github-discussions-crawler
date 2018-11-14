package org.qualiton.crawler.testsupport.scalatest

import org.scalatest.{ BeforeAndAfterAll, Suite }

import org.qualiton.crawler.testsupport.resource.ResourceSupport
import org.qualiton.crawler.testsupport.wiremock.SlackIncomingWebhookMockServer

trait SlackIncomingWebhookMockServerSupport
  extends BeforeAndAfterAll
    with ResourceSupport {
  this: Suite =>

  val mockSlackIncomingWebhookMockServer = new SlackIncomingWebhookMockServer()

  override def beforeAll(): Unit = {
    mockSlackIncomingWebhookMockServer.startMockServer()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    mockSlackIncomingWebhookMockServer.stopMockServer()
    super.afterAll()
  }
}

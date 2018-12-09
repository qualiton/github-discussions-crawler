package org.qualiton.slack.testsupport.scalatest

import cats.scalatest.EitherValues

import org.scalatest.{ BeforeAndAfterAll, Suite }

import org.qualiton.slack.testsupport.wiremock.SlackRtmApiMockServer

trait SlackRtmApiMockServerSupport
  extends BeforeAndAfterAll
    with EitherValues {
  this: Suite =>

  val slackRtmApiMockServer = new SlackRtmApiMockServer()

  override def beforeAll(): Unit = {
    slackRtmApiMockServer.startMockServer()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    slackRtmApiMockServer.stopMockServer()
    super.afterAll()
  }
}



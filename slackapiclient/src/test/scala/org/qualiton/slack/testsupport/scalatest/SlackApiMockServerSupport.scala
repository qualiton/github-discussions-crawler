package org.qualiton.slack.testsupport.scalatest

import cats.scalatest.EitherValues

import org.scalatest.{ BeforeAndAfterAll, Suite }

import org.qualiton.slack.testsupport.wiremock.SlackApiMockServer

trait SlackApiMockServerSupport
  extends BeforeAndAfterAll
    with EitherValues {
  this: Suite =>

  val slackApiMockServer = new SlackApiMockServer()

  override def beforeAll(): Unit = {
    slackApiMockServer.startMockServer()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    slackApiMockServer.stopMockServer()
    super.afterAll()
  }
}



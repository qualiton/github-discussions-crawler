package org.qualiton.crawler.testsupport.scalatest

import cats.scalatest.EitherValues

import org.scalatest.{ BeforeAndAfterAll, Suite }

import org.qualiton.crawler.testsupport.resource.ResourceSupport
import org.qualiton.crawler.testsupport.wiremock.GithubApiV3MockServer

trait GithubApiV3MockServerSupport
  extends BeforeAndAfterAll
    with EitherValues
    with ResourceSupport {
  this: Suite =>

  val mockGithubApiV3MockServer = new GithubApiV3MockServer()

  override def beforeAll(): Unit = {
    mockGithubApiV3MockServer.startMockServer()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    mockGithubApiV3MockServer.stopMockServer()
    super.afterAll()
  }
}

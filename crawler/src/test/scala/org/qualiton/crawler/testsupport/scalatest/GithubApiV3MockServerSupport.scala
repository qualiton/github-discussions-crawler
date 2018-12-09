package org.qualiton.crawler.testsupport.scalatest

import org.scalatest.{ BeforeAndAfterAll, Suite }

import org.qualiton.crawler.testsupport.resource.ResourceSupport
import org.qualiton.crawler.testsupport.wiremock.GithubApiV3MockServer

trait GithubApiV3MockServerSupport
  extends BeforeAndAfterAll
    with ResourceSupport {
  this: Suite =>

  val githubApiV3MockServer = new GithubApiV3MockServer()

  override def beforeAll(): Unit = {
    githubApiV3MockServer.startMockServer()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    githubApiV3MockServer.stopMockServer()
    super.afterAll()
  }
}

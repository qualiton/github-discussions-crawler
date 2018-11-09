package org.qualiton.crawler

import cats.scalatest.{ EitherMatchers, ValidatedValues }

import org.apache.http.HttpStatus
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually

import org.qualiton.crawler.testsupport.scalatest.GithubApiV3MockServerSupport

class CrawlerEndToEndSpec
  extends FeatureSpec
    with GivenWhenThen
    with Matchers
    with EitherValues
    with EitherMatchers
    with OptionValues
    with ValidatedValues
    with Eventually
    with TypeCheckedTripleEquals
    with GithubApiV3MockServerSupport
    with Inside {

  feature("Internal status endpoint") {
    scenario("Status should return successfully") {
      When("Hitting the status endpoint")
      Then("we should receive empty body")

      mockGithubApiV3MockServer.mockDiscussions(1, 1, 2, HttpStatus.SC_OK)
    }
  }
}

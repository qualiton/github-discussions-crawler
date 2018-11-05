package org.qualiton.crawler

import cats.scalatest.{ EitherMatchers, ValidatedValues }

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ Matchers, _ }
import org.scalatest.concurrent.Eventually

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
    with Inside {

  feature("Internal status endpoint") {
    scenario("Status should return successfully") {
      When("Hitting the status endpoint")
      Then("we should receive empty body")
    }
  }
}

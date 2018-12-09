package org.qualiton.crawler.common.testsupport

import cats.scalatest.{ EitherMatchers, EitherValues, ValidatedMatchers, ValidatedValues }

import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, FreeSpec, Inside, Matchers, OptionValues }
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }

trait FreeSpecSupport extends FreeSpec
  with Matchers
  with BeforeAndAfterAll
  with EitherValues
  with EitherMatchers
  with OptionValues
  with ValidatedMatchers
  with ValidatedValues
  with MockFactory
  with Eventually
  with TypeCheckedTripleEquals
  with Inside {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(15, Seconds),
      interval = Span(100, Millis))
}

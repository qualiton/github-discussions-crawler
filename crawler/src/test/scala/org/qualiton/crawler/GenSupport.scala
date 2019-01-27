package org.qualiton.crawler

import java.time.Instant

import com.mifmif.common.regex.Generex
import eu.timepit.refined.api.RefType
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import eu.timepit.refined.scalacheck.{ AnyInstances, BooleanInstances, CharInstances, CollectionInstances, GenericInstances, NumericInstances, RefTypeInstances, StringInstances }
import eu.timepit.refined.string.{ MatchesRegex, Url => RefinedUrl }
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.{ Arbitrary, Gen, ScalacheckShapeless }
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import shapeless.Witness

import org.qualiton.crawler.domain.core.Url

trait GenSupport
  extends GeneratorDrivenPropertyChecks
    with AnyInstances
    with BooleanInstances
    with CharInstances
    with CollectionInstances
    with GenericInstances
    with NumericInstances
    with RefTypeInstances
    with StringInstances
    with ScalacheckShapeless {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 50)

  def arbitrary[T](implicit a: Arbitrary[T]): Gen[T] = Arbitrary.arbitrary[T]

  val instantGen: Gen[Instant] = {
    val minTimestamp = Instant.parse("2000-01-01T00:00:00.000Z").toEpochMilli
    val maxTimestamp = Instant.parse("2031-01-01T00:00:00.000Z").toEpochMilli

    Gen.choose(minTimestamp, maxTimestamp).map(Instant.ofEpochMilli)
  }

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary(instantGen)

  val printableNonEmptyCharGen: Gen[Char] =
    Gen.choose(33.toChar, 126.toChar)

  val printableCharGen: Gen[Char] =
    Gen.oneOf(printableNonEmptyCharGen, Gen.const(' '))

  val printableNonEmptyStringGen: Gen[String] =
    Gen.nonEmptyListOf(printableNonEmptyCharGen).map(_.mkString)

  val urlGen: Gen[Url] = for {
    host <- Gen.alphaChar
    port <- arbitrary[UserPortNumber]
  } yield refineV[RefinedUrl](s"http://$host:$port")
    .fold(e => throw new IllegalStateException(s"Error generating a valid Url with $host and $port, $e"), identity)

  implicit val arbNonEmptyString: Arbitrary[NonEmptyString] =
    Arbitrary(printableNonEmptyStringGen.map(refineV[NonEmpty](_)
      .fold(e => throw new IllegalStateException(s"Error generating a valid NonEmptyString: $e"), identity)))

  implicit val arbUrl: Arbitrary[Url] = Arbitrary(urlGen)

  implicit def refinedRegexArbitrary[F[_, _] : RefType, S](implicit wit: Witness.Aux[S], ev: S <:< String): Arbitrary[F[String, MatchesRegex[S]]] = {

    //Generex does not like anchored regexes at all.
    def unanchor(s: String): String = s.stripPrefix("^").stripSuffix("$")

    val gen = Gen.posNum[Long].map { seed =>
      val regGen: Generex = new Generex(unanchor(ev(wit.value)))
      regGen.setSeed(seed)
      regGen.random()
    }

    arbitraryRefType[F, String, MatchesRegex[S]](gen)
  }
}

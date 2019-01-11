package org.qualiton.crawler

import org.scalacheck.{ Arbitrary, Gen }

import org.qualiton.crawler.domain.git.Discussion

trait DiscussionGen extends GenSupport {

  val discussionGen: Gen[Discussion] = arbitrary[Discussion]

  implicit val arbDiscussion: Arbitrary[Discussion] = Arbitrary(discussionGen)
}

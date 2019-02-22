package org.qualiton.crawler

import org.scalacheck.{ Arbitrary, Gen }

import org.qualiton.crawler.domain.git.TeamDiscussionAggregateRoot

trait DiscussionGen extends GenSupport {

  val discussionGen: Gen[TeamDiscussionAggregateRoot] = arbitrary[TeamDiscussionAggregateRoot]

  implicit val arbDiscussion: Arbitrary[TeamDiscussionAggregateRoot] = Arbitrary(discussionGen)
}

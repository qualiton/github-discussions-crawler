package org.qualiton.crawler.domain.slack

import org.http4s.Status
import org.qualiton.crawler.domain.core.Event

trait SlackClient[F[_]] {

  def sendDiscussionEvent(event: Event): F[Status]

}

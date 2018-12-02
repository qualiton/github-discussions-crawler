package org.qualiton.crawler.domain.core

trait EventPublisher[F[_]] {

  def publishDiscussionEvent(event: Event): F[Unit]

}

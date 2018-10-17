package org.qualiton.crawler.common.config

import cats.Show

final case class Secret[T](value: T) {
  override def toString: String = "Secret(***)"
}

object Secret {
  implicit def showSecret[T]: Show[Secret[T]] =
    Show.fromToString
}

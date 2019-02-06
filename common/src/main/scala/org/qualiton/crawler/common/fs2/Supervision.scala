package org.qualiton.crawler
package common.fs2

import cats.effect.Sync
import cats.syntax.applicativeError._
import fs2.{ Stream, Pipe => Fs2Pipe }

import com.typesafe.scalalogging.LazyLogging

object Supervision extends LazyLogging {

  def logAndRestartOnError[F[_] : Sync, A](channel: String): Fs2Pipe[F, A, A] = { in =>
    def restart: Stream[F, A] = in.onError {
      case e =>
        Stream.eval(logger.warn(s"Restarting $channel stream", e).delay)
    }
      .handleErrorWith(e => restart)

    restart
  }
}

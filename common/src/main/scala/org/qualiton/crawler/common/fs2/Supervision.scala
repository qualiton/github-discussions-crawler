package org.qualiton.crawler
package common.fs2

import cats.effect.Sync
import cats.syntax.applicativeError._
import fs2.{ Stream, Pipe => Fs2Pipe }
import io.chrisdavenport.log4cats.Logger

object Supervision {

  def logAndRestartOnError[F[_] : Sync : Logger, A](channel: String): Fs2Pipe[F, A, A] = { in =>
    def restart: Stream[F, A] = in.onError {
      case e =>
        Stream.eval(Logger[F].error(e)(s"Restarting $channel stream"))
    }
      .handleErrorWith(e => restart)

    restart
  }
}

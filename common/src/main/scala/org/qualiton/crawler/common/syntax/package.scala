package org.qualiton.crawler
package common

import cats.effect.Sync
import fs2.Stream

package object syntax extends syntax.AnySyntax

package syntax {

  object any extends AnySyntax

  object delay extends DelaySyntax

  object all extends AllSyntax

  object effect extends EffectSyntax

  trait AllSyntax
    extends AnySyntax
      with DelaySyntax
      with EffectSyntax

  trait AnySyntax {
    implicit class Pipe[A](t: A) {
      def |>[B](f: A => B): B = f(t)

      def into[B](f: A => B): B = f(t)
    }
  }

  trait DelaySyntax {
    implicit class AnyOps[A](a: => A) {
      def delay[F[_] : Sync]: F[A] = Sync[F].delay(a)
    }
  }

  trait EffectSyntax {
    implicit class EffectOps[F[_] : Sync, A](effect: F[A]) {
      def stream: Stream[F, A] = Stream.eval(effect)
    }
  }
}

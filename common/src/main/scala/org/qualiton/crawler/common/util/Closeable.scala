package org.qualiton.crawler.common.util

trait Closeable[F[_]] {

  def close: F[Unit]

}

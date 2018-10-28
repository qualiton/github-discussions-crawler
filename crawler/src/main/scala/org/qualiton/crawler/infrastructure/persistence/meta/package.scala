package org.qualiton.crawler.infrastructure.persistence

import scala.reflect.runtime.universe._

import doobie.util.Meta
import io.circe.{ Decoder, Encoder, Json }
import io.circe.syntax._

package object meta extends MetaInstances {

  def codecMeta[A: Encoder : Decoder : TypeTag]: Meta[A] =
    Meta[Json].timap[A](a => a.as[A].fold[A](throw _, identity))(a => a.asJson)

}

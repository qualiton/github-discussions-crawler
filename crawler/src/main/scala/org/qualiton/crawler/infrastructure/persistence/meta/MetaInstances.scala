package org.qualiton.crawler.infrastructure.persistence.meta

import cats.syntax.either._

import doobie.Meta
import io.circe.Json
import io.circe.jawn.parse
import org.postgresql.util.PGobject

/**
  * https://tpolecat.github.io/doobie/docs/12-Custom-Mappings.html
  */
trait MetaInstances {
  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced.other[PGobject]("json").timap[Json](
      a => parse(a.getValue).leftMap[Json](e => throw e).merge)(
      a => {
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      }
    )
}

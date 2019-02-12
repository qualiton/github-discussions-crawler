package org.qualiton.crawler.infrastructure.rest.git

import org.http4s.headers.Link
import org.scalactic.TripleEqualsSupport
import org.scalatest.OptionValues

import org.qualiton.crawler.common.testsupport.FreeSpecSupport

class LinkSpec extends FreeSpecSupport with TripleEqualsSupport with OptionValues {

  val link = """https://api.github.com/teams/2356612/discussions?direction=asc&per_page=2&page=1>; rel="prev", <https://api.github.com/teams/2356612/discussions?direction=asc&per_page=2&page=3>; rel="next", <https://api.github.com/teams/2356612/discussions?direction=asc&per_page=2&page=18>; rel="last", <https://api.github.com/teams/2356612/discussions?direction=asc&per_page=2&page=1>; rel="first"""""

  "extract" - {
    "`next` from format RFC 5988" in {
      val result = link.split(",").toList.flatMap(i => Link.parse(i.trim).toOption).find(_.rel == Some("next")).map(_.uri)
      result.value.toString() should ===("https://api.github.com/teams/2356612/discussions?direction=asc&per_page=2&page=3")
    }
  }
}

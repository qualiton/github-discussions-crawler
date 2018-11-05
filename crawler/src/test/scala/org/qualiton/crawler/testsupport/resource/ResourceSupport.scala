package org.qualiton.crawler.testsupport.resource

import scala.io.Source

trait ResourceSupport {

  def loadResource(resource: String): String =
    Source.fromURL(getClass.getResource(resource), "UTF8").mkString

}

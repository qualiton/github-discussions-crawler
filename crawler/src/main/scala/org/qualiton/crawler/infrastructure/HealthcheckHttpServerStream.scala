package org.qualiton.crawler.infrastructure

import cats.effect.{ ConcurrentEffect, ExitCode }
import fs2.Stream

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.net.UserPortNumber
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder

object HealthcheckHttpServerStream extends {

  def apply[F[_] : ConcurrentEffect](httpPort: UserPortNumber): Stream[F, ExitCode] = {

    object dsl extends Http4sDsl[F]
    import dsl._

    val statusRoute = HttpRoutes.of[F] {
      case GET -> Root / "internal" / "status" => Ok()
    }

    val serverBuild = BlazeBuilder[F].bindHttp(httpPort, "0.0.0.0")

    serverBuild.mountService(statusRoute, "/").serve
  }

}

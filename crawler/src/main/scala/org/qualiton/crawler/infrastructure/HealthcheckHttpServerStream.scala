package org.qualiton.crawler.infrastructure

import cats.effect.{ ConcurrentEffect, ExitCode, Timer }
import fs2.Stream

import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.net.UserPortNumber
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Router

object HealthcheckHttpServerStream extends {

  def apply[F[_] : ConcurrentEffect : Timer](httpPort: UserPortNumber): Stream[F, ExitCode] = {

    object dsl extends Http4sDsl[F]
    import dsl._

    val statusRoute = HttpRoutes.of[F] {
      case GET -> Root / "internal" / "status" => Ok()
    }

    val serverBuild = BlazeServerBuilder[F].bindHttp(httpPort, "0.0.0.0")

    serverBuild.withHttpApp(Router("/" -> statusRoute).orNotFound).serve
  }

}

package org.qualiton.crawler.testsupport.dockerkit

import java.util.Properties

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

import com.whisk.docker.{ DockerCommandExecutor, DockerContainerState, DockerReadyChecker }

private[dockerkit] class PostgresDockerReadyChecker(user: String, password: String, port: Int)
  extends DockerReadyChecker {

  override def apply(container: DockerContainerState)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext): Future[Boolean] = {
    import java.sql.DriverManager
    val url = s"jdbc:postgresql://localhost:$port/postgres"
    val props = new Properties()
    props.setProperty("user", user)
    props.setProperty("loggerLevel", "OFF")
    props.setProperty("password", password)
    val conn = () =>
      Try(
        DriverManager
          .getConnection(url, props)
          .close()
      )

    Future(conn().isSuccess)
  }
}

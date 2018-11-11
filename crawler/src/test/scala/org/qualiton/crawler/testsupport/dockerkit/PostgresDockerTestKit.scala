package org.qualiton.crawler.testsupport.dockerkit

import scala.concurrent.duration._

import com.whisk.docker.{ DockerContainer, DockerKit }
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.TestSuite

trait PostgresDockerTestKit
  extends DockerKitSpotify
    with DockerTestKit
    with DockerKit {
  self: TestSuite =>

  type PortMapping = (Int, Option[Int])
  type EnvironmentVariable = (String, String)

  def postgresVersion: String = "9.6-alpine"

  lazy val postgresAdvertisedPort: Int = 5432

  final val defaultPostgresUser: String = "postgres"

  def postgresPassword: String = "postgres"

  final val postgresContainerPort: Int = 5432

  def postgresPortMappings: Seq[PortMapping] =
    Seq(postgresContainerPort -> Some(postgresAdvertisedPort))

  def postgresEnv: Seq[EnvironmentVariable] =
    Seq(s"POSTGRES_PASSWORD" -> postgresPassword)

  def postgresContainer: DockerContainer =
    DockerContainer(s"postgres:$postgresVersion")
      .withPorts(postgresPortMappings: _*)
      .withEnv(postgresEnv.map(toDockerEnvironmentArgument): _*)
      .withReadyChecker(new PostgresDockerReadyChecker(defaultPostgresUser, postgresPassword, postgresAdvertisedPort).looped(2, 5.seconds))

  override def dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers

  private def toDockerEnvironmentArgument(environmentVariable: EnvironmentVariable): String =
    environmentVariable match {
      case (name, value) => s"$name=$value"
    }

}

import com.typesafe.sbt.SbtNativePackager.autoImport.{ maintainer, packageDescription, packageName, packageSummary }
import com.typesafe.sbt.packager.Keys.{ dockerAlias, dockerBuildOptions, dockerUpdateLatest }
import com.typesafe.sbt.packager.archetypes.scripts.AshScriptPlugin
import com.typesafe.sbt.packager.docker.{ Cmd, DockerAlias, DockerPlugin }
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ Docker => TypesafeDocker, _ }
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import sbt.{ AutoPlugin, Compile, Project }
import sbt.Keys._

object Docker extends AutoPlugin {

  private val default =
    Seq(
      javaOptions in Universal ++= Seq(
        "-J-XX:+UnlockExperimentalVMOptions",
        "-J-XX:+UseCGroupMemoryLimitForHeap",
        "-J-XX:MaxRAMFraction=2",
        "-J-XX:+PrintFlagsFinal",
        "-J-XX:+PrintGCDateStamps",
        "-J-XX:+PrintGCDetails",
        "-J-XX:+UseGCLogFileRotation",
        "-J-XX:NumberOfGCLogFiles=3",
        "-J-XX:GCLogFileSize=2M",
        "-J-XX:+HeapDumpOnOutOfMemoryError",
        "-Xloggc:/tmp/gc.log"),
      aggregate in TypesafeDocker := false,
      mainClass in Compile := Some("org.qualiton.crawler.main.Main"),
      dockerBaseImage := "frolvlad/alpine-oraclejdk8:latest", // use alpine based image with glibc for sigar
      dockerUpdateLatest := true,
      dockerBuildOptions := "--rm=false" +: dockerBuildOptions.value.tail,
      packageName in TypesafeDocker := moduleName.value,
      packageSummary in TypesafeDocker := name.value,
      packageDescription in TypesafeDocker := description.value,
      maintainer in TypesafeDocker := "info@qualiton.org",
      dockerAlias := {
        DockerAlias(
          registryHost = sys.env.get("GCR_PREFIX") orElse Some("eu.gcr.io"),
          username = sys.env.get("NONPROD_PROJECT_ID") orElse Some("qualiton-nonprod"),
          name = sys.env.getOrElse("CONTAINER_NAME", moduleName.value),
          tag = Some(version.value)
        )
      },
      dockerCommands := {
        val extraDockerCommands = Seq(Cmd("RUN", "apk --update add dumb-init"))
        dockerCommands.value.head +: extraDockerCommands ++: dockerCommands.value.tail
      },
      dockerEntrypoint := Seq("/usr/bin/dumb-init", "--single-child", "--") ++ dockerEntrypoint.value)

  object autoImport {

    implicit final class DockerSettings(val project: Project) extends AnyVal {
      def withDocker: Project =
        project
          .enablePlugins(AshScriptPlugin, DockerPlugin)
          .settings(default)
    }

  }

}

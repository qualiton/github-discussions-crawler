import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys.bashScriptExtraDefines
import sbt._
import sbt.Keys._
import sbt.librarymanagement.DependencyFilter

object Alpn extends AutoPlugin {

  val AlpnBootVersion = "8.1.12.v20180117"
  val AlpnApiVersion = "1.1.3.v20160715"

  val alpnDependencies: Seq[Def.Setting[_]] =
    libraryDependencies ++= Seq(
      "org.eclipse.jetty.alpn" % "alpn-api" % AlpnApiVersion,
      "org.mortbay.jetty.alpn" % "alpn-boot" % AlpnBootVersion)

  private val buildAlpnSettings: Seq[Def.Setting[_]] = {

    val alpnBootFilter: DependencyFilter = artifactFilter("alpn-boot") && artifactFilter(`type` = "jar")

    def findAlpnBoot(report: UpdateReport): File = report.matching(alpnBootFilter).head

    alpnDependencies ++
    Seq(
      mappings in Universal += findAlpnBoot(update.value) -> s"alpn/alpn-boot-$AlpnBootVersion.jar",
      bashScriptExtraDefines += s"""addJava "-Xbootclasspath/p:$${app_home}/../alpn/alpn-boot-$AlpnBootVersion.jar""""
    )
  }

  private val runtimeAlpnSettings: Seq[Def.Setting[_]] = {
    def addAlpnPath(attList: Keys.Classpath): Seq[String] = for {
      file <- attList.map(_.data)
      path = file.getAbsolutePath if path.contains("alpn-boot")
    } yield "-Xbootclasspath/p:" + path

    alpnDependencies ++
    Seq(
      javaOptions in run ++= addAlpnPath((managedClasspath in Runtime).value)
    )
  }

  object autoImport {

    implicit final class AlpnProject(val project: Project) extends AnyVal {
      def withAlpn: Project =
        project
          .settings(buildAlpnSettings)

      def withLocalAlpn: Project =
        project
          .settings(runtimeAlpnSettings)
    }

  }

}

import sbt._
import sbt.Keys._

object TestConfig extends AutoPlugin {

  private lazy val EndToEndTest: Configuration = config("e2e") extend Test
  private lazy val IntegrationTest: Configuration = config("it") extend Test

  private lazy val e2e = taskKey[Unit]("Run end to end tests")
  private lazy val it = taskKey[Unit]("Run integration tests")

  object autoImport {

    implicit final class TestProject(val project: Project) extends AnyVal {
      def withTestConfig(coverageMinimumPercent: Int = 100): Project =
        project
          .configs(EndToEndTest, IntegrationTest)
          .settings(javaOptions in Test += "-Duser.timezone=UTC")
          .settings(integrationTestSettings, endToEndTestSettings)
          .settings(scoverageSettings(coverageMinimumPercent))
          .settings(
            e2e := (test in EndToEndTest).value,
            it := (test in IntegrationTest).value)
    }
  }

  private lazy val integrationTestSettings: Seq[Def.Setting[_]] =
    inConfig(IntegrationTest)(Defaults.testSettings) ++
    Seq(
      scalaSource in IntegrationTest := baseDirectory.value / "src/it/scala",
      parallelExecution in IntegrationTest := false,
      fork in IntegrationTest := true)

  private lazy val endToEndTestSettings: Seq[Def.Setting[_]] =
    inConfig(EndToEndTest)(Defaults.testSettings) ++
    Seq(
      scalaSource in EndToEndTest := baseDirectory.value / "src/e2e/scala",
      parallelExecution in EndToEndTest := false,
      fork in EndToEndTest := true)

  private def scoverageSettings(coverageMinimumValue: Int): Seq[Def.Setting[_]] =
    Seq(
      coverageEnabled := true,
      coverageMinimum := coverageMinimumValue,
      coverageFailOnMinimum := true)
}

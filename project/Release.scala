import sbt.{ file, AutoPlugin, IO, Project, ThisBuild }
import sbt.Keys.{ baseDirectory, moduleName, version }
import sbtrelease.ReleasePlugin.autoImport.{ releaseCommitMessage, releaseProcess, releaseStepCommand, releaseTagComment, releaseTagName, releaseUseGlobalVersion, ReleaseStep, ReleaseTransformations }

object Release extends AutoPlugin {

  object autoImport {

    implicit final class ReleaseSettings(val project: Project) extends AnyVal {
      def withRelease: Project =
        project
          .settings(releaseSettings)
    }

  }

  import ReleaseTransformations._

  private lazy val releaseSettings = Seq(
    releaseUseGlobalVersion := true,
    releaseTagName := s"v${ (version in ThisBuild).value }",
    releaseTagComment := s"Release version ${ (version in ThisBuild).value }",
    releaseCommitMessage := s"[ci skip] Set version to ${ (version in ThisBuild).value }",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      updateHelmChartReleaseVersion,
      releaseStepCommand("docker:publishLocal"),
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

  private val updateHelmChartReleaseVersion = ReleaseStep(action = state => {
    // extract the build state
    val extracted = Project.extract(state)

    val vcsOpt = sbtrelease.Vcs.detect(extracted.get(baseDirectory))
    val chartFile = file(s".helm/${ extracted.get(moduleName) }/Chart.yaml")

    val withNewVersion = IO.read(chartFile)
      .replaceAll(
        "version:[^\\n]+",
        s"version: ${ extracted.get(version) }"
      )

    IO.write(chartFile, withNewVersion)

    vcsOpt.foreach { vcs =>
      vcs.add(chartFile.getAbsolutePath).!
      vcs.commit("Update helm chart to latest version [ci skip]", sign = false).!
    }

    state
  })
}

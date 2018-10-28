import sbt.{ Def, _ }
import sbt.Keys._

object Dependencies extends AutoPlugin {

  private val CatsVersion = "1.4.0"
  private val CatsEffectVersion = "1.0.0"
  private val CatsMtl = "0.4.0"
  private val CirisVersion = "0.11.0"
  private val DockerTestkitVersion = "0.9.6"
  private val Http4sVersion = "0.19.0"
  private val DoobieVersion = "0.6.0"
  private val RefinedVersion = "0.9.2"
  private val CirceVersion = "0.10.0"
  private val MonocleVersion = "1.5.1-cats"

  private val cats = Seq(
    "org.typelevel" %% "cats-core" % CatsVersion,
    "org.typelevel" %% "cats-effect" % CatsEffectVersion,
    "org.typelevel" %% "cats-mtl-core" % CatsMtl)

  private val ciris =
    Seq(
      "core",
      "enumeratum",
      "generic",
      "refined")
      .map(module => "is.cir" %% s"ciris-$module" % CirisVersion)

  private val google =
    Seq(
      "com.google.guava" % "guava" % "25.0-jre")

  private val circe =
    Seq(
      "core",
      "generic",
      "parser",
      "literal",
      "java8",
      "generic-extras",
      "refined",
      "fs2")
      .map(module => "io.circe" %% s"circe-$module" % CirceVersion)

  private val http4s =
    Seq(
      "blaze-server",
      "circe",
      "dsl",
      "blaze-client")
      .map(module => "org.http4s" %% s"http4s-$module" % Http4sVersion)

  private val logging =
    Seq(
      "org.slf4j" % "jul-to-slf4j" % "1.7.25",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2")

  private val doobie =
    Seq(
      "core",
      "hikari",
      "postgres")
      .map(module => "org.tpolecat" %% s"doobie-$module" % DoobieVersion)

  private val database =
    Seq(
      "com.zaxxer" % "HikariCP" % "2.7.8",
      "org.postgresql" % "postgresql" % "42.2.2",
      "org.flywaydb" % "flyway-core" % "5.0.7",
      "com.google.cloud.sql" % "postgres-socket-factory" % "1.0.5" exclude ("com.google.guava", "guava-jdk5"))

  private val monocle =
    Seq(
      "core",
      "macro")
      .map(module => "com.github.julien-truffaut" %% s"monocle-$module" % MonocleVersion)

  private val enumeratum =
    Seq(
      "com.beachape" %% "enumeratum" % "1.5.13",
      "com.beachape" %% "enumeratum-scalacheck" % "1.5.15",
      "com.beachape" %% "enumeratum-circe" % "1.5.17",
      "com.beachape" %% "enumeratum-macros" % "1.5.9")

  private val others =
    Seq(
      "eu.timepit" %% "refined" % RefinedVersion,
      "eu.timepit" %% "refined-cats" % RefinedVersion,
      "org.scalactic" %% "scalactic" % "3.0.5")

  private val test =
    Seq(
      "org.scalatest" %% "scalatest" % "3.0.5",
      "org.scalacheck" %% "scalacheck" % "1.14.0",
      "com.github.mifmif" % "generex" % "1.0.2",
      "com.ironcorelabs" %% "cats-scalatest" % "2.3.1",
      "eu.timepit" %% "refined-scalacheck" % RefinedVersion,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6",
      "org.scalamock" %% "scalamock" % "4.1.0",
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
      "com.whisk" %% "docker-testkit-impl-spotify" % DockerTestkitVersion,
      "com.whisk" %% "docker-testkit-scalatest" % DockerTestkitVersion,
      "com.spotify" % "docker-client" % "8.11.2")

  private val defaultDependencies: Seq[Def.Setting[Seq[ModuleID]]] =
    Seq(
      libraryDependencies ++=
      cats ++
      ciris ++
      circe ++
      monocle ++
      enumeratum ++
      database ++
      doobie ++
      google ++
      http4s ++
      others ++
      logging)

  object autoImport {

    implicit final class DependenciesProject(val project: Project) extends AnyVal {
      def withDependencies: Project =
        project
          .settings(defaultDependencies)
          .settings(addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"))
    }

  }

}

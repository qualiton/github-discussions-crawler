import sbt.Keys._
import sbt.{Def, _}

object BaseSettings {

  val default: Seq[Def.Setting[_]] =
    Seq(
      organization := "org.qualiton",
      organizationName := "Qualiton Ltd",
      organizationHomepage := Some(url("https://www.qualiton.org/")),
      name := "Github Discussions Crawler",
      moduleName := "github-discussions-crawler",
      description := "Collects git discussion info and publishes on Slack",
      scalaVersion := "2.12.7",
      shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ " }
    ) ++ Aliases.aliases
}

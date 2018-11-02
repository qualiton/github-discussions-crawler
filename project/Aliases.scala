import sbt.{ Def, _ }
import sbt.Keys._

object Aliases extends AutoPlugin {

  override def trigger = allRequirements

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    super.projectSettings ++
    Seq(fork := true)

  lazy val aliases: Seq[Def.Setting[State => State]] =
    addCommandAlias("runServer", ";project server ;clean ;runMain org.qualiton.crawler.server.main.Main") ++
    addCommandAlias("all-tests", ";clean ;test ;it ;e2e")
}

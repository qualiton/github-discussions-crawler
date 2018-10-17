import sbt.{ Resolver, _ }
import sbt.Keys._

object Resolvers {

  val default =
    Seq(
      credentials += Credentials(Path.userHome / ".bintray-credentials"),
      resolvers ++=
      Seq(
        "confluent" at "http://packages.confluent.io/maven/",
        Resolver.bintrayRepo("ovotech", "maven"),
        Resolver.bintrayRepo("ovotech", "maven-private"),
        Resolver.bintrayRepo("gabrielasman", "maven")
      )
    )

}

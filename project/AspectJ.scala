import com.lightbend.sbt.javaagent.JavaAgent
import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.javaAgents
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import sbt.{ AutoPlugin, Project }
import sbt.Keys.javaOptions
import sbt.librarymanagement.DependencyBuilders

object AspectJ extends AutoPlugin with DependencyBuilders {

  object autoImport {

    implicit final class AspectJSettings(val project: Project) extends AnyVal {

      def withAspectJ: Project =
        project
          .enablePlugins(JavaAgent)
          .settings(
            Seq(
              javaAgents += "org.aspectj" % "aspectjweaver" % "1.9.2",
              javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default"
            )
          )
    }

  }
}

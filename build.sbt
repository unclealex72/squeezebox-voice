import com.typesafe.sbt.packager.docker._
import ReleaseTransformations._

name := """squeezebox-voice"""
organization := "uk.co.unclealex"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin, AshScriptPlugin)

scalaVersion := "2.12.3"
scalacOptions += "-target:jvm-1.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

dockerBaseImage := "openjdk:alpine"
dockerExposedPorts := Seq(9443)
maintainer := "Alex Jones <alex.jones@unclealex.co.uk>"
dockerUsername := Some("unclealex72")
version in Docker := "latest"
dockerCommands := {
  val commands = dockerCommands.value
  val (prefixCommands, suffixCommands) = commands.splitAt {
    val firstRunCommand = commands.indexWhere {
      case Cmd("ADD", _) => true
      case _ => false
    }
    firstRunCommand + 1
  }
  val extraCommands = Seq(
    Cmd("RUN", "mkdir", "-p", "/opt/docker/ext"),
    Cmd("VOLUME", "/opt/docker/ext/"))
  prefixCommands ++ extraCommands ++ suffixCommands
}
javaOptions in Universal ++= Seq("-Dhttps.port=9443", "-Dhttp.port=disabled")

publish := { (publish in Docker).value }

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  releaseStepCommand("docker:publish"),   // : ReleaseStep, build server docker image.
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
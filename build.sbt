import com.typesafe.sbt.packager.docker._

name := """squeezebox-voice"""
organization := "uk.co.unclealex"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin, AshScriptPlugin)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

dockerBaseImage := "openjdk:alpine"
dockerExposedPorts := Seq(9443)
maintainer := "Alex Jones <alex.jones@unclealex.co.uk>"
dockerRepository := Some("unclealex72")
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
    Cmd("RUN", "mkdir", "-p", "/opt/docker/ssl"),
    Cmd("VOLUME", "/opt/docker/ssl/"))
  prefixCommands ++ extraCommands ++ suffixCommands
}
javaOptions in Universal ++= Seq("-Dhttps.port=9443")

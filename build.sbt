name := """squeezebox-voice"""
organization := "uk.co.unclealex"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// Oauth
libraryDependencies ++= Seq(
  "com.nulab-inc" %% "scala-oauth2-core" % "1.3.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "1.3.0"
)

// Database
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "org.postgresql" % "postgresql" % "42.1.4"
)

// Login page
libraryDependencies ++= Seq(
  "org.webjars" % "bootstrap" % "3.2.0-2",
  "org.webjars" % "jquery" % "3.2.1"
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "uk.co.unclealex.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "uk.co.unclealex.binders._"

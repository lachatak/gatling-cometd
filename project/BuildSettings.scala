import sbt.Keys._
import sbt._

object BuildSettings {

  lazy val basicSettings = Seq(
    version := "1.0.0-SNAPSHOT",
    name := "http-cometd",
    organization := "org.kaloz.gatling",
    organizationHomepage := Some(new URL("http://waytothepiratecove.blogspot.co.uk")),
    description := "Gatling cometD extension",
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scalaVersion := "2.10.4",
    crossPaths := false,
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.7",
      "-language:postfixOps",
      "-language:implicitConversions"
    ),
    javacOptions := Seq(
      "-Xlint:deprecation",
      "-encoding",
      "utf8",
      "-XX:MaxPermSize=256M"
    )
  )
}
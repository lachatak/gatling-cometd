import BuildSettings._
import Dependencies._
import Publish._
import Release._
import io.gatling.sbt.GatlingPlugin
import sbt.Keys._
import sbt._

object CometDBuild extends Build {

  override lazy val settings = super.settings :+ {
    shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ "}
  }

  lazy val cometd = project.in(file("."))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= cometDDeps)
    .settings(sonatypeSettings: _*)
    .settings(releaseToBintraySettings: _*)
    .enablePlugins(GatlingPlugin)
}

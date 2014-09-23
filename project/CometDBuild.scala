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

  lazy val root = Project("root", file("."))
    .aggregate(cometd, example)
    .settings(basicSettings: _*)
    .settings(noPublishing: _*)

  lazy val cometd = Project("http-cometd", file("http-cometd"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= cometDDeps)
    .settings(sonatypeSettings: _*)
    .settings(releaseToBintraySettings: _*)

  lazy val example = Project("http-cometd-example", file("http-cometd-example"))
    .aggregate(cometd)
    .dependsOn(cometd)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= cometDExampleDeps)
    .settings(noPublishing: _*)
    .enablePlugins(GatlingPlugin)
}

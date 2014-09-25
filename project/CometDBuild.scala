import BuildSettings._
import Dependencies._
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
    .settings(startServerTask)

  lazy val cometd = Project("http-cometd", file("http-cometd"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= cometDDeps)

  lazy val example = Project("http-cometd-example", file("http-cometd-example"))
    .aggregate(cometd)
    .dependsOn(cometd)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= cometDExampleDeps)
    .settings(noPublishing: _*)
    .enablePlugins(GatlingPlugin)

  lazy val startServer = TaskKey[Unit]("startServer", "run server.js with nodejs")

  val startServerTask: Setting[Task[Unit]] = startServer := {
    val pb = Process( """node http-cometd-example/server.js""")
    pb.!
  }

}

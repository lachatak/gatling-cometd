import io.gatling.sbt.GatlingPlugin
import xerial.sbt.Sonatype.SonatypeKeys._

sonatypeSettings

name := "http-cometd"

organization := "org.kaloz.gatling"

profileName := "lachatak"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.7",
  "-language:postfixOps",
  "-language:implicitConversions")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8", "-XX:MaxPermSize=256M")

crossPaths := false

val test = project.in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.0.0-RC3",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.0",
      "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.4.1",
      "io.gatling" % "test-framework" % "1.0-RC1" % "test",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
      "org.specs2" %% "specs2" % "2.3.7" % "test",
      "org.scalatest" %% "scalatest" % "2.2.0" % "test"
    ))

publishMavenStyle := true

pomIncludeRepository := { _ => false}

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("https://github.com/lachatak/gatling-cometd"))

pomExtra := (
  <scm>
    <url>git@github.com:lachatak/gatling-cometd.git</url>
    <connection>scm:git:git@github.com:lachatak/gatling-cometd.git</connection>
  </scm>
    <developers>
      <developer>
        <id>lachatak</id>
        <name>Krisztian Lachata</name>
        <email>krisztian.lachata@gmail.com</email>
        <url>http://waytothepiratecove.blogspot.co.uk</url>
      </developer>
    </developers>)
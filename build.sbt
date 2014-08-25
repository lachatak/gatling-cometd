name := "gatling-test"

organization := "org.kaloz.excercise"

version := "1.0.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8", "-XX:MaxPermSize=256M")

crossPaths := false

val test = project.in(file("."))
  .settings(libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.2",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.4.1",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.0.0-SNAPSHOT" % "test"
))

resolvers += "sonatype snapshot" at "https://oss.sonatype.org/content/repositories/snapshots"
    
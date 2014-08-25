name := "gatling-test"

organization := "org.kaloz.excercise"

version := "1.0.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8", "-XX:MaxPermSize=256M")

crossPaths := false

val test = project.in(file("."))
  .settings(libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.0.0-SNAPSHOT" % "test"
))

resolvers += "sonatype snapshot" at "https://oss.sonatype.org/content/repositories/snapshots"
    
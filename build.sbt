

name := "cometd"

organization := "org.kaloz.gatling"

version := "1.0.0"

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
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.0.0-SNAPSHOT",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.0",
      "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.4.1",
      "io.gatling" % "test-framework" % "1.0-RC1" % "test"
    ))

resolvers ++= Seq(
  "Sonatype snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typasafe release" at "http://repo.typesafe.com/typesafe/releases/",
  "SonaType release" at "https://oss.sonatype.org/content/repositories/releases/"
)

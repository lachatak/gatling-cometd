import sbt._

object Version {

  val gatling       = "2.1.0-SNAPSHOT"
  val gatlingTest   = "1.0"
  val akka          = "2.3.4"
  val jackson       = "2.4.1"
  val scalaTest     = "2.2.0"
  val spec2         = "2.3.12"
}

object Library {
  val gatling         = "io.gatling.highcharts"          %  "gatling-charts-highcharts"      % Version.gatling
  val jacksonCore     = "com.fasterxml.jackson.core"     %  "jackson-databind"               % Version.jackson
  val jacksonDatatype = "com.fasterxml.jackson.datatype" %  "jackson-datatype-joda"          % Version.jackson
  val jacksonModule   = "com.fasterxml.jackson.module"   %  "jackson-module-scala_2.10"      % Version.jackson
  val gatlingTest     = "io.gatling"                     %  "test-framework"                 % Version.gatlingTest
  val akkaTestkit     = "com.typesafe.akka"              %% "akka-testkit"                   % Version.akka
  val scalaTest       = "org.scalatest"                  %% "scalatest"                      % Version.scalaTest
  val spec2           = "org.specs2"                     %% "specs2"                         % Version.spec2
}

object Dependencies {

  import Library._

  val cometDDeps = List(
    gatling,
    jacksonCore,
    jacksonDatatype,
    jacksonModule,
    akkaTestkit       % "test",
    spec2             % "test",
    scalaTest         % "test"
  )

  val cometDExampleDeps = List(
    gatlingTest       % "test"
  )
}

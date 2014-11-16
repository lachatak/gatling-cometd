package org.kaloz.gatling.http.cometd.test

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.StrictLogging
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._
import org.kaloz.gatling.http.action.cometd.PushProcessorActor
import org.kaloz.gatling.http.cometd.CometDMessages.PublishedMap
import org.kaloz.gatling.http.cometd.Predef._

import scala.concurrent.duration._

object CometDGatlingTest extends App {
  val props = new GatlingPropertiesBuilder
  props.dataDirectory(IDEPathHelper.dataDirectory.toString)
  props.resultsDirectory(IDEPathHelper.resultsDirectory.toString)
  props.binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString)
  props.mute()
  Gatling.fromMap(props.build)
}

class CometDGatlingTest extends Simulation with StrictLogging {

  case class Shout(message: String = "Echo message!!", userId: String = "${userId}", correlationId: String = "${correlationId}")

  implicit val requestTimeOut = 5 seconds
  val users = 2

  val userIdGenerator = new AtomicLong(1)
  val idGenerator = new AtomicLong(1)

  val httpConf = http
    .wsBaseURL("ws://localhost:8000")
    .wsReconnect
    .wsMaxReconnects(3)
    .disableFollowRedirect
    .disableWarmUp

  val userIdFeeder = Iterator.continually(Map("userId" -> userIdGenerator.getAndIncrement()))
  val idFeeder = Iterator.continually(Map("id" -> idGenerator.getAndIncrement))
  val uuidFeeder = Iterator.continually(Map("correlationId" -> UUID.randomUUID.toString))

  val scn = scenario("cometD")
    .feed(userIdFeeder)
    .pause(1, 6)

    .exec(cometD("Open").open("/bayeux").pushProcessor[TimerCounterProcessor])
    .feed(idFeeder).exec(cometD("Handshake").handshake())
    .doIf(session => session.contains("clientId")) {
    feed(idFeeder).exec(cometD("Connect").connect())

      .feed(idFeeder).exec(cometD("Subscribe Timer").subscribe("/timer/${userId}").acceptPushContains(Set("TriggeredTime")))
      .feed(idFeeder).exec(cometD("Subscribe Echo").subscribe("/echo/${userId}"))

      .asLongAs(session => session("counter").asOption[Long].getOrElse(0l) < 4) {
      pause(1, 2).feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
    }
      .asLongAs(session => session("counter").asOption[Long].getOrElse(0l) < 8) {
      pause(1, 2).exec(cometD("reconciliate").reconciliate)
    }
      .pause(1, 2).feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))

      .feed(idFeeder).exec(cometD("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
      .feed(idFeeder).exec(cometD("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
      .feed(idFeeder).exec(cometD("Disconnect").disconnect())
  }
  //    .exec(cometD("Close cometD").close)

  setUp(scn.inject(rampUsers(users) over 30).protocols(httpConf))
    .assertions(global.successfulRequests.percent.is(100)
    )
}

class TimerCounterProcessor extends PushProcessorActor {

  val counter = new AtomicLong(0)

  override def sessionUpdates = {
    case PublishedMap(channel, data) =>
      Map("counter" -> counter.getAndIncrement)
  }
}
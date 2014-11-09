package org.kaloz.gatling.http.cometd.test

import java.util.concurrent.atomic.AtomicLong
import java.util.{Date, UUID}

import akka.actor.ActorRef
import com.typesafe.scalalogging.slf4j.Logging
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._
import org.kaloz.gatling.http.action.cometd.PushProcessorActor
import org.kaloz.gatling.http.action.cometd.SessionHandler.Store
import org.kaloz.gatling.http.cometd.CometDMessages.PublishedMap
import org.kaloz.gatling.http.cometd.Predef._

import scala.concurrent.duration._

object CometDGatlingTest extends App {
  val gatlingPropertyBuilder = new GatlingPropertiesBuilder
  gatlingPropertyBuilder.simulationClass(classOf[CometDGatlingTest].getName)
  Gatling.fromMap(gatlingPropertyBuilder.build)
}

class CometDGatlingTest extends Simulation with Logging {

  case class Shout(message: String = "Echo message!!", userId: String = "${userId}", correlationId: String = "${correlationId}", timestamp: Date = new Date)

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

class TimerCounterProcessor(sessionHandler: ActorRef) extends PushProcessorActor {

  val counter = new AtomicLong(0)

  override def messageReceive = {
    case PublishedMap(channel, data) =>
      //      log.info(s"Process $data")
      sessionHandler ! Store(Map("counter" -> counter.getAndIncrement))
  }
}
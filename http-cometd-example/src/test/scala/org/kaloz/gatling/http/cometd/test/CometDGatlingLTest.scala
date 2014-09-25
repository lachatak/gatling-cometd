package org.kaloz.gatling.http.cometd.test

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, Props}
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logging
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._
import org.kaloz.gatling.http.action.cometd.PubSubProcessorActor
import org.kaloz.gatling.http.cometd.CometDMessages.PublishedMap
import org.kaloz.gatling.http.cometd.Predef._
import org.kaloz.gatling.http.cometd.test.Processor.GetCounter

import scala.concurrent.Await
import scala.concurrent.duration._

object CometDGatlingTest extends App {
  val gatlingPropertyBuilder = new GatlingPropertiesBuilder
  gatlingPropertyBuilder.simulationClass(classOf[CometDGatlingTest].getName)
  Gatling.fromMap(gatlingPropertyBuilder.build)
}

class CometDGatlingTest extends Simulation with Logging {

  case class Shout(message: String = "Echo message!!", userId: String = "${userId}", correlationId: String = "${correlationId}")

  val processor = GatlingActorSystem.instance.actorOf(Processor.props, name = "Processor")
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

  val userIdFeeder = Iterator.continually(Map("userId" -> userIdGenerator.getAndIncrement))
  val idFeeder = Iterator.continually(Map("id" -> idGenerator.getAndIncrement))
  val uuidFeeder = Iterator.continually(Map("correlationId" -> UUID.randomUUID.toString))

  val scn = scenario("cometD")
    .feed(userIdFeeder)

    .exec(cometD("Open").open("/bayeux"))
    .feed(idFeeder).exec(cometD("Handshake").handshake())
    .doIf(session => session.contains("clientId")) {
    feed(idFeeder).exec(cometD("Connect").connect())

      .feed(idFeeder).exec(cometD("Subscribe Timer").subscribe("/timer/${userId}", Set("TriggeredTime")))
      .feed(idFeeder).exec(cometD("Subscribe Echo").subscribe("/echo/${userId}", subscribeToPubSubProcessor = false))

      .asLongAs(session => {
      implicit val timeout = Timeout(5 seconds)
      import akka.pattern.ask

      val counterFuture = (processor ? GetCounter).mapTo[Long]
      val counter = Await.result(counterFuture, timeout.duration)
      counter < 5
    }) {
      feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(matchers = Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
        .pause(2, 4)
    }

      .feed(idFeeder).exec(cometD("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
      .feed(idFeeder).exec(cometD("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
      .feed(idFeeder).exec(cometD("Disconnect").disconnect())
  }
  //    .exec(cometD("Close cometD").close)

  setUp(scn.inject(rampUsers(users) over 1).protocols(httpConf))
    .assertions(global.successfulRequests.percent.is(100)
    )
}

class Processor extends PubSubProcessorActor {

  val counter = new AtomicLong(0)

  def messageReceive: Actor.Receive = {
    case PublishedMap(channel, data) =>
      counter.getAndIncrement
    case GetCounter =>
      sender ! counter.get
  }
}

object Processor {
  def props: Props = Props[Processor]

  case object GetCounter

}

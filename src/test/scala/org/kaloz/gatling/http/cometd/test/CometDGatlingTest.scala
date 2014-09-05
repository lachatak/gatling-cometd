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
import org.kaloz.gatling.http.cometd.test.Processor.GetCounter

import scala.concurrent.Await
import scala.concurrent.duration._

object CometDGatlingTest extends App {
  val gatlingPropertyBuilder = new GatlingPropertiesBuilder
  gatlingPropertyBuilder.simulationClass(classOf[CometDGatlingTest].getName)
  Gatling.fromMap(gatlingPropertyBuilder.build)
}

class CometDGatlingTest extends Simulation with Logging {

  import org.kaloz.gatling.http.cometd._

  case class Shout(message: String = "Echo message!!", userId: String = "${userId}", correlationId: String = "${correlationId}")

  val processor = GatlingActorSystem.instance.actorOf(Processor.props, name = "Processor")
  implicit val requestTimeOut = 5 seconds
  val users = 1

  val userIdGenerator = new AtomicLong(1)
  val idGenerator = new AtomicLong(1)

  def nextUserId = userIdGenerator.getAndIncrement

  def nextId = idGenerator.getAndIncrement

  def nextUUID = UUID.randomUUID.toString

  val httpConf = http
    .wsBaseURL("ws://localhost:8000")
    .wsReconnect
    .wsMaxReconnects(3)
    .disableFollowRedirect
    .disableWarmUp

  val scn = scenario("WebSocket")
    .exec(storeValue("userId", nextUserId))

    .exec(cometd("Open").open("/beyaux").registerPubSubProcessor)
    .exec(storeValue("id", nextId)).exec(cometd("Handshake").handshake())
    .exec(storeValue("id", nextId)).exec(cometd("Connect").connect())

    .exec(storeValue("id", nextId)).exec(cometd("Subscribe Timer").subscribe("/timer", List("TriggeredTime")))
    .exec(storeValue("id", nextId)).exec(cometd("Subscribe Echo").subscribe("/echo/${userId}", subscribeToPubSubProcessor = false))

    .asLongAs(session => {
    implicit val timeout = Timeout(5 seconds)
    import akka.pattern.ask

    val counterFuture = (processor ? GetCounter).mapTo[Long]
    val counter = Await.result(counterFuture, timeout.duration)
    counter < 5
  }) {
    exec(storeValue("correlationId", nextUUID)).exec(cometd("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(matchers = List("${correlationId}", "EchoedMessage","!!egassem ohcE")))
      .pause(3, 5)
  }

    .exec(storeValue("id", nextId)).exec(cometd("Unsubscribe Timer").unsubscribe("/timer"))
    .exec(storeValue("id", nextId)).exec(cometd("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .exec(storeValue("id", nextId)).exec(cometd("Disconnect").disconnect())
  //    .exec(ws("Close WS").close)

  setUp(
    scn.inject(rampUsers(users) over 1)
      .protocols(httpConf)
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

package org.kaloz.gatling.http.cometd.test

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, Props}
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._
import org.kaloz.gatling.http.action.cometd.PubSubProcessorActor
import org.kaloz.gatling.http.cometd.CometDMessages.{Data, Published}
import org.kaloz.gatling.http.cometd.test.Processor.GetCounter

import scala.concurrent.Await
import scala.concurrent.duration._

object CometDGatlingTest extends App {
  val gatlingPropertyBuilder = new GatlingPropertiesBuilder
  gatlingPropertyBuilder.simulationClass(classOf[CometDGatlingTest].getName)
  Gatling.fromMap(gatlingPropertyBuilder.build)
}

class CometDGatlingTest extends Simulation with StrictLogging {

  import org.kaloz.gatling.http.cometd._

  case class Shout(message: String = "Echo message!!", userId: String = "${userId}") extends Data

  val processor = GatlingActorSystem.instance.actorOf(Processor.props, name = "Processor")
  implicit val requestTimeOut = 5 seconds
  val users = 50
  val userId = new AtomicLong(1)

  val httpConf = http
    .wsBaseURL("ws://localhost:8000")
    .wsReconnect
    .wsMaxReconnects(3)
    .disableFollowRedirect
    .disableWarmUp

  val scn = scenario("WebSocket")
    .exec(cometd("Open").open("/beyaux").registerPubSubProcessor)
    .exec(cometd("Handshake").handshake)
    .exec(cometd("Connect").connect)

    .exec(session => session.set("userId", userId.getAndIncrement))
    .exec(cometd("Subscribe Timer").subscribe("/timer", Some("TriggeredTime")))
    .exec(cometd("Subscribe Echo").subscribe("/echo/${userId}", subscribeToPubSubProcessor = false))

    .asLongAs(session => {
    implicit val timeout = Timeout(5 seconds)
    import akka.pattern.ask

    val counterFuture = (processor ? GetCounter).mapTo[Long]
    val counter = Await.result(counterFuture, timeout.duration)
    counter < 5
  }) {
    exec(cometd("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(m => if (m.contains("EchoedMessage")) m else ""))
      .pause(3, 5)
  }

    .exec(cometd("Unsubscribe Timer").unsubscribe("/timer"))
    .exec(cometd("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .exec(cometd("Disconnect").disconnect)
  //    .exec(ws("Close WS").close)

  setUp(
    scn.inject(rampUsers(users) over 1)
      .protocols(httpConf)
  )
}

class Processor extends PubSubProcessorActor {

  val counter = new AtomicLong(0)

  def messageReceive: Actor.Receive = {
    case Published(channel, data) =>
      counter.getAndIncrement
    case GetCounter =>
      sender ! counter.get
  }
}

object Processor {
  def props: Props = Props[Processor]

  case object GetCounter

}

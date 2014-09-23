package org.kaloz.gatling.http.action.cometd

import java.util.Date

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.ning.http.client.websocket.WebSocket
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.{GatlingConfiguration, GatlingPropertiesBuilder}
import io.gatling.core.result.writer.{DataWriter, RunMessage}
import io.gatling.core.session.Session
import io.gatling.http.action.ws.{OnOpen, OnTextMessage}
import io.gatling.http.ahc.WsTx
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CometDActorITest extends TestKit(ActorSystem("GatlingTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll
with ImplicitSender
with MockitoSugar {

  override def afterAll() {
    system.shutdown()
  }

  "CometDActor" should {
    "publish all incoming messages to the event stream" in new scope {

      cometDActor ! OnTextMessage("TEXT", new Date().getTime)

      listener.underlyingActor.message should be(Some("TEXT"))
    }
  }

  private trait scope {

    val gatlingPropertyBuilder = new GatlingPropertiesBuilder
    GatlingConfiguration.setUp(gatlingPropertyBuilder.build)
    GatlingActorSystem.start
    val runMessage = mock[RunMessage]
    val replyTo = TestProbe()
    when(runMessage.runDescription).thenReturn("Run")
    DataWriter.init(runMessage, Seq(), replyTo.ref)

    val wsTx = mock[WsTx]
    val session = mock[Session]
    val webSocket = mock[WebSocket]
    val next = TestProbe()
    val check = None

    when(wsTx.session).thenReturn(session)
    when(wsTx.next).thenReturn(next.ref)
    when(wsTx.check).thenReturn(check)
    when(wsTx.copy(session)).thenReturn(wsTx)
    when(session.set(anyString(), isA(classOf[ActorRef]))).thenReturn(session)

    val cometDActor = TestActorRef(new CometDActor("name"))
    val listener = TestActorRef(new Listener)
    cometDActor ! OnOpen(wsTx, webSocket, 1000)

  }

}

class Listener extends Actor {
  override def preStart = {
    context.system.eventStream.subscribe(context.self, classOf[Message])
  }

  var message: Option[String] = None

  override def receive: Receive = {
    case Message(m) => message = Some(m)
  }
}

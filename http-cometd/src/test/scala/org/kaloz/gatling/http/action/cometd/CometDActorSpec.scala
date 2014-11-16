package org.kaloz.gatling.http.action.cometd

import java.util.Date

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.ning.http.client.Request
import com.ning.http.client.websocket.WebSocket
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.result.writer.{DataWriter, RunMessage}
import io.gatling.core.session.Session
import io.gatling.http.action.ws.{OnOpen, OnTextMessage, Reconciliate}
import io.gatling.http.ahc.WsTx
import io.gatling.http.config.HttpProtocol
import org.kaloz.gatling.Fixture
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.{Message, SessionUpdates}
import org.kaloz.gatling.http.cometd.CometDMessages.PublishedMap
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag

class CometDActorSpec extends TestKit(ActorSystem("GatlingTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll
with ImplicitSender
with MockitoSugar {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  override def beforeAll {
    io.gatling.ConfigHook.setUpForTest()

    GatlingActorSystem.start
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
    GatlingActorSystem.shutdown
  }

  "CometDActor" should {

    "not have a push processor if we don't pass processor type in the constructor" in new scope {

      val cometDActor = TestActorRef(new CometDActorStub)

      cometDActor.underlyingActor.pushProcessor should be(None)
    }

    "set None push processor to the session when processing OnOpen message" in new scope {

      val cometDActor = TestActorRef(new CometDActorStub)

      cometDActor ! OnOpen(wsTx, webSocket, 1000)

      cometDActor.underlyingActor.wsTx.get.session.attributes(PushProcessorActor.PushProcessorName) should be(None)
    }

    "have a push processor if we pass processor type in the constructor" in new scope {

      val cometDActor = TestActorRef(new CometDActorStub(Some(ClassTag(classOf[SaveCallPushProcessorActorStub]))))

      cometDActor.underlyingActor.pushProcessor should not be (None)
    }

    "set the push processor option to the session when processing OnOpen message" in new scope {

      val cometDActor = TestActorRef(new CometDActorStub(Some(ClassTag(classOf[SaveCallPushProcessorActorStub]))))

      cometDActor ! OnOpen(wsTx, webSocket, 1000)

      cometDActor.underlyingActor.wsTx.get.session.attributes(PushProcessorActor.PushProcessorName) should not be (None)
    }

    "pass incoming messages to the processor which belongs to the session" in new scope {

      implicit val ec = ExecutionContext.Implicits.global

      val cometDActor = TestActorRef(new CometDActor("name", Some(ClassTag(classOf[SaveCallPushProcessorActorStub]))))

      cometDActor ! OnOpen(wsTx, webSocket, 1000)

      cometDActor ! OnTextMessage(Fixture.testJsonString, new Date().getTime)

      cometDActor.underlyingActor.pushProcessor.get.ask(GetData)(5 seconds).mapTo[Option[String]].onSuccess {
        case response =>
          response should not be (None)
      }
    }

    "process SessionUpdate message and pile up session modifications" in new scope {

      val cometDActor = TestActorRef(new CometDActor("name"))

      cometDActor ! OnOpen(wsTx, webSocket, 1000)

      cometDActor ! SessionUpdates(List((session: Session) => session.set("test", "testvalue"), (session: Session) => session.set("test2", 10)))

      val next = TestProbe()
      cometDActor ! Reconciliate("reconciliate", next.ref, session)

      next.expectMsgPF() {
        case session: Session =>
          session.attributes.getOrElse("test", "NONE") should be("testvalue")
          session.attributes.getOrElse("test2", "NONE") should be(10)

        case _ => fail()
      }
    }
  }

  private trait scope {

    val runMessage = RunMessage("simulationClassName", "simulationId", io.gatling.core.util.TimeHelper.nowMillis, "Run")
    val replyTo = TestProbe()
    DataWriter.init(Seq(), runMessage, Seq(), replyTo.ref)

    val session = Session("scenarioName", "userId")
    val wsTx = WsTx(session, mock[Request], "requestName", mock[HttpProtocol], TestProbe().ref, new Date().getTime)
    val webSocket = mock[WebSocket]
  }

}

class CometDActorStub(pushProcessorManifest: Option[ClassTag[_]] = None) extends CometDActor("name", pushProcessorManifest) {

  var wsTx: Option[WsTx] = None

  override val initialState: Receive = {
    case o@OnOpen(tx, webSocket, end) =>
      wsTx = Some(tx)
  }
}

class SaveCallPushProcessorActorStub extends PushProcessorActor {

  var data: Option[String] = None

  override def receive: Actor.Receive = requestData orElse process()

  override def sessionUpdates = {
    case PublishedMap(channel, d) =>
      Map.empty
  }

  val requestData: Receive = {
    case Message(text) =>
      data = Some(text)
    case GetData => sender ! data
  }
}

case object GetData

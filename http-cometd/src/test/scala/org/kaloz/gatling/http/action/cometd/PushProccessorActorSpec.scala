package org.kaloz.gatling.http.action.cometd

import akka.actor._
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import io.gatling.core.session.Session
import org.kaloz.gatling.Fixture._
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.{Message, SessionUpdates, SubscribeMessage, UnsubscribeMessage}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PushProccessorActorSpec extends TestKit(ActorSystem("GatlingTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "PushProccessorActor" must {

    "be able to handle an incoming message if it has a valid subscription" in new scope {

      pushProccessorActor ! Message(testJsonString)

      parent.expectMsgPF() {
        case SessionUpdates(l@List(_, _)) => {
          l should have size 2

          val newSession = session.update(l)
          newSession.attributes.getOrElse("message", "NONE") should be("value")
          newSession.attributes.getOrElse("content", 0) should be(10)

          newSession.attributes should have size 2
        }
        case _ => fail()
      }
    }

    "be able to handle unsubscription" in new scope {

      pushProccessorActor ! UnsubscribeMessage("/test")

      pushProccessorActor ! Message(testJsonString)

      parent.expectNoMsg()
    }

    "not handle an incoming message if it hasn't got a valid subscription" in new scope {

      pushProccessorActor ! Message( """{"message":"value","other":{"elem1":"value1","elem2":10}}""")

      parent.expectNoMsg()
    }
  }

  private trait scope {

    io.gatling.ConfigHook.setUpForTest()

    val parent = TestProbe()
    val pushProccessorActor = TestActorRef(Props[PushProccessorActorStub], parent.ref, "pushProcessor")

    import org.kaloz.gatling.json.JsonMarshallableImplicits._

    var extractor = (m: String) => m.fromJson[TestObject]
    pushProccessorActor ! SubscribeMessage("/test", Set("content"), extractor)

    val session = Session("scenarioName", "userId")
  }

}

class PushProccessorActorStub extends PushProcessorActor {

  override def messageReceive = {
    case TestObject(message, content) =>
      Map("message" -> message, "content" -> content("elem2"))
  }
}


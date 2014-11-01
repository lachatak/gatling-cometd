package org.kaloz.gatling.http.action.cometd

import akka.actor._
import akka.testkit.{TestActorRef, TestKit}
import io.gatling.core.Predef._
import org.kaloz.gatling.Fixture.{TestObject, testJsonString}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PubSubProccessorActorITest extends TestKit(ActorSystem("GatlingTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "PubSubProccessorActor" should {
    "be able to handle a new subscription" in new scope {

      pubSubProccessorActor.underlyingActor.subscriptions should have size 1
      pubSubProccessorActor.underlyingActor.subscriptions.get("/test").get._1.toString should be("(?=.*content).*")
      pubSubProccessorActor.underlyingActor.subscriptions.get("/test").get._2 should be(extractor)
    }

    "be able to handle unsubscription" in new scope {

      pubSubProccessorActor.underlyingActor.subscriptions should have size 1

      system.eventStream.publish(UnsubscribeMessage("/test"))
      pubSubProccessorActor.underlyingActor.subscriptions should have size 0
    }

    "be able to delegate extracted message" in new scope {

      pubSubProccessorActor.underlyingActor.subscriptions should have size 1

      system.eventStream.publish(Message(testJsonString))

      pubSubProccessorActor.underlyingActor.received should be(Some(TestObject("value", Map("elem1" -> "value1", "elem2" -> 10))))
    }

    "not delegate extracted message if it hasn't got subscription" in new scope {

      system.eventStream.publish(UnsubscribeMessage("/test"))
      pubSubProccessorActor.underlyingActor.subscriptions should have size 0

      system.eventStream.publish(Message(testJsonString))

      pubSubProccessorActor.underlyingActor.received should be(None)
    }
  }

  private trait scope {

    io.gatling.ConfigHook.setUpForTest()

    import org.kaloz.gatling.json.JsonMarshallableImplicits._

    val pubSubProccessorActor = TestActorRef(new PubSubProccessorActorStub)

    var extractor = (m: String) => m.fromJson[TestObject]
    system.eventStream.publish(SubscribeMessage("/test", Set("content"), extractor))
  }

  class PubSubProccessorActorStub extends PubSubProcessorActor {

    var received: Option[TestObject] = None

    override def messageReceive: Receive = {
      case t@TestObject(_, _) => received = Some(t)

    }
  }

}


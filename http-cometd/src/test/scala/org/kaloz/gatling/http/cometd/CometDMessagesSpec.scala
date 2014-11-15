package org.kaloz.gatling.http.cometd

import org.junit.runner.RunWith
import org.kaloz.gatling.http.cometd.CometDMessages._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations


@RunWith(classOf[JUnitRunner])
class CometDMessagesSpec extends Specification with AllExpectations {

  "Handshake" should {

    val handshake = Handshake()

    "have a '/meta/handshake' channel type" in {
      handshake.channel mustEqual "/meta/handshake"
    }

    "have an optional ${id} id" in {
      handshake.id.isDefined mustEqual true
      handshake.id.get mustEqual "${id}"
    }
  }

  "Connect" should {

    val connect = Connect()

    "have a '/meta/connect' channel type" in {
      connect.channel mustEqual "/meta/connect"
    }

    "have an optional ${id} id" in {
      connect.id.isDefined mustEqual true
      connect.id.get mustEqual "${id}"
    }

    "have an ${clientId} clientId" in {
      connect.clientId mustEqual "${clientId}"
    }
  }

  "Disconnect" should {

    val disconnect = Disconnect()

    "have a '/meta/disconnect' channel type" in {
      disconnect.channel mustEqual "/meta/disconnect"
    }

    "have an optional ${id} id" in {
      disconnect.id.isDefined mustEqual true
      disconnect.id.get mustEqual "${id}"
    }

    "have an ${clientId} clientId" in {
      disconnect.clientId mustEqual "${clientId}"
    }
  }

  "Subscribe" should {

    val subscribe = Subscribe(subscription = "test")

    "have a '/meta/subscribe' channel type" in {
      subscribe.channel mustEqual "/meta/subscribe"
    }

    "have an optional ${id} id" in {
      subscribe.id.isDefined mustEqual true
      subscribe.id.get mustEqual "${id}"
    }

    "have an ${clientId} clientId" in {
      subscribe.clientId mustEqual "${clientId}"
    }
  }

  "Unsubscribe" should {

    val unsubscribe = Unsubscribe(subscription = "test")

    "have a '/meta/unsubscribe' channel type" in {
      unsubscribe.channel mustEqual "/meta/unsubscribe"
    }

    "have an optional ${id} id" in {
      unsubscribe.id.isDefined mustEqual true
      unsubscribe.id.get mustEqual "${id}"
    }

    "have an ${clientId} clientId" in {
      unsubscribe.clientId mustEqual "${clientId}"
    }
  }

  "Publish" should {

    "have an ${clientId} clientId" in {
      val publish = Publish("channel", "data")

      publish.clientId mustEqual "${clientId}"
    }
  }
}

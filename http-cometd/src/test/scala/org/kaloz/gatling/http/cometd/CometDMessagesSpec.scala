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

    "have an ${cometDMessageId} in field id" in {
      handshake.id mustEqual "${cometDMessageId}"
    }
  }

  "Connect" should {

    val connect = Connect()

    "have a '/meta/connect' channel type" in {
      connect.channel mustEqual "/meta/connect"
    }

    "have an ${cometDMessageId} in field id" in {
      connect.id mustEqual "${cometDMessageId}"
    }

    "have an ${cometDClientId} in field clientId" in {
      connect.clientId mustEqual "${cometDClientId}"
    }
  }

  "Disconnect" should {

    val disconnect = Disconnect()

    "have a '/meta/disconnect' channel type" in {
      disconnect.channel mustEqual "/meta/disconnect"
    }

    "have an ${cometDMessageId} in field id" in {
      disconnect.id mustEqual "${cometDMessageId}"
    }

    "have an ${cometDClientId} in field clientId" in {
      disconnect.clientId mustEqual "${cometDClientId}"
    }
  }

  "Subscribe" should {

    val subscribe = Subscribe(subscription = "test")

    "have a '/meta/subscribe' channel type" in {
      subscribe.channel mustEqual "/meta/subscribe"
    }

    "have an ${cometDMessageId} in field id" in {
      subscribe.id mustEqual "${cometDMessageId}"
    }

    "have an ${cometDClientId} in field clientId" in {
      subscribe.clientId mustEqual "${cometDClientId}"
    }
  }

  "Unsubscribe" should {

    val unsubscribe = Unsubscribe(subscription = "test")

    "have a '/meta/unsubscribe' channel type" in {
      unsubscribe.channel mustEqual "/meta/unsubscribe"
    }

    "have an ${cometDMessageId} in field id" in {
      unsubscribe.id mustEqual "${cometDMessageId}"
    }

    "have an ${cometDClientId} in field clientId" in {
      unsubscribe.clientId mustEqual "${cometDClientId}"
    }
  }

  "Publish" should {

    "have an ${cometDClientId} in field clientId" in {
      val publish = Publish("channel", "data")

      publish.clientId mustEqual "${cometDClientId}"
    }
  }
}

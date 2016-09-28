package org.kaloz.gatling.http.cometd

object CometDMessages {

  trait Published

  case class Ack(channel: String, clientId: Option[String], successful: Boolean, id: Option[String], subscription: Option[String])

  case class Handshake(channel: String = "/meta/handshake", version: String = "1.0", supportedConnectionTypes: List[String] = List("websocket", "long-polling", "callback-polling"), minimumVersion: Option[String] = Some("0.9"), id: String = "${cometDMessageId}", advice: Option[Map[String, Any]] = Some(Map("timeout" -> 60000, "interval" -> 0)), ext: Option[Any] = None)

  case class Connect(channel: String = "/meta/connect", clientId: String = "${cometDClientId}", connectionType: String = "websocket", id: String = "${cometDMessageId}", advice: Option[Map[String, Any]] = Some(Map("timeout" -> 0)))

  case class Disconnect(channel: String = "/meta/disconnect", clientId: String = "${cometDClientId}", id: String = "${cometDMessageId}")

  case class Subscribe(channel: String = "/meta/subscribe", clientId: String = "${cometDClientId}", subscription: String, id: String = "${cometDMessageId}", ext: Option[Any] = None)

  case class Unsubscribe(channel: String = "/meta/unsubscribe", clientId: String = "${cometDClientId}", subscription: String, id: String = "${cometDMessageId}")

  case class Publish(channel: String, data: Any, clientId: String = "${cometDClientId}", ext: Option[Any] = None)

  case class PublishedMap(channel: String, data: Map[String, Any]) extends Published

}
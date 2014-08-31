package org.kaloz.gatling.http.cometd

object CometDMessages {

  trait Ext

  trait Data

  case class Ack(channel: String, clientId: Option[String], successful: Boolean, id: Option[String])

  case class Handshake(channel: String = "/meta/handshake", version: String = "1.0", supportedConnectionTypes: List[String] = List("websocket", "long-polling", "callback-polling"), minimumVersion: Option[String] = Some("0.9"), id: Option[String] = Some("1"), advice: Option[Map[String, Any]] = Some(Map("timeout" -> 60000, "interval" -> 0)), ext: Option[Ext] = None)

  //the advice behave strange
  case class Connect(channel: String = "/meta/connect", clientId: String = "${clientId}", connectionType: String = "websocket", id: Option[String] = Some("2"), advice: Option[Map[String, Any]] = Some(Map("timeout" -> 0)))

  case class Disconnect(channel: String = "/meta/disconnect", clientId: String = "${clientId}", id: Option[String] = Some("3"))

  case class Subscribe(channel: String = "/meta/subscribe", clientId: String = "${clientId}", subscription: String, id: Option[String] = Some("4"))

  case class Unsubscribe(channel: String = "/meta/unsubscribe", clientId: String = "${clientId}", subscription: String, id: Option[String] = Some("5"))

  case class Publish(channel: String, data: Data, clientId: Option[String] = None, id: Option[String] = None, ext: Option[Ext] = None)

  case class Published(channel: String, data: Map[String, Any])

}

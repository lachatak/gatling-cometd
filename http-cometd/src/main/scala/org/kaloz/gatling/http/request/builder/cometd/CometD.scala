package org.kaloz.gatling.http.request.builder.cometd

import io.gatling.core.session._
import io.gatling.http.action.ws._
import io.gatling.http.request.builder.CommonAttributes
import org.kaloz.gatling.http.action.cometd._
import org.kaloz.gatling.http.cometd.CometDMessages._

import scala.concurrent.duration.FiniteDuration

object CometD {

  val DefaultCometDName = SessionPrivateAttributes.PrivateAttributePrefix + "http.cometd"
}

class CometD(val requestName: Expression[String], val cometDName: String = CometD.DefaultCometDName)(implicit requestTimeOut: FiniteDuration) {

  def cometDName(cometDName: String) = new CometD(requestName, cometDName)

  def open(url: Expression[String]) = new CometDOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), cometDName)

  def handshake(handshake: Handshake = Handshake()) = new CometDHandshakeActionBuilder(requestName, cometDName, handshake)

  def connect(connect: Connect = Connect()) = new CometDConnectActionBuilder(requestName, cometDName, connect)

  def subscribe(subscription: String, ext: Option[Any] = None)) = new CometDSubscribeActionBuilderStep1(requestName, cometDName, Subscribe(subscription = subscription, ext = ext))

  def unsubscribe(subscription: String) = new CometDUnsubscribeActionBuilder(requestName, cometDName, Unsubscribe(subscription = subscription))

  def publish(channel: String, data: Any) = new CometDPublishActionBuilderStep1(requestName, cometDName, Publish(channel, data))

  def sendCommand(channel: String, data: Any) = publish(channel, data)

  def disconnect(disconnect: Disconnect = Disconnect()) = new CometDDisconnectActionBuilder(requestName, cometDName, disconnect)

  def reconciliate = new WsReconciliateActionBuilder(requestName, cometDName)

  def close = new WsCloseActionBuilder(requestName, cometDName)

}

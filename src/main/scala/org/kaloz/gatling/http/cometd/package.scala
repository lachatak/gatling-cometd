package org.kaloz.gatling.http

import io.gatling.core.Predef._
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.check._
import io.gatling.core.session._
import io.gatling.http.Predef._
import io.gatling.http.action.ws._
import io.gatling.http.check.ws.WsCheck
import io.gatling.http.request.builder.ws.{Ws, WsOpenRequestBuilder}
import org.kaloz.gatling.http.action.cometd.{SubscribeMessage, UnsubscribeMessage}
import org.kaloz.gatling.http.cometd.CometDMessages._
import org.kaloz.gatling.http.request.builder.cometd.CometDOpenRequestBuilder

import scala.concurrent.duration._

package object cometd {

  def cometd(requestName: Expression[String]) = new Ws(requestName)

  implicit class WsOpenRequestBuilder2CometDBuilder(val wsOpenRequestBuilder: WsOpenRequestBuilder) {
    def registerPubSubProcessor = {
      new CometDOpenRequestBuilder(wsOpenRequestBuilder.commonAttributes, wsOpenRequestBuilder.wsName)
    }
  }

  implicit class WsCometdExtension(val ws: Ws)(implicit requestTimeOut: FiniteDuration) {

    import org.kaloz.gatling.json.MarshallableImplicits._

    def handshake(handshake: Handshake = Handshake()) = {
      ws.sendText(handshake.toJson).checkResponse(check(_, { (ack, message) => ack.clientId.getOrElse("")}), saveAs = Some("clientId"))
    }

    def connect(connect: Connect = Connect()) = {
      ws.sendText(connect.toJson).checkResponse(check(_, { (ack, message) => message}))
    }

    def subscribe(subscription: String, responsePattern: Option[String] = None, subscribeToPubSubProcessor: Boolean = true, extractor:String=>Published = {m=> m.fromJson[List[PublishedMap]].get(0)}) = {
      ws.sendText(Subscribe(subscription = subscription).toJson).checkResponse(check(_, { (ack, message) =>
        if (subscribeToPubSubProcessor)
          GatlingActorSystem.instance.eventStream.publish(SubscribeMessage(subscription, responsePattern.getOrElse( s""""channel":"$subscription""""), extractor))
        message
      }))
    }

    def unsubscribe(subscription: String) = {
      ws.sendText(Unsubscribe(subscription = subscription).toJson).checkResponse(check(_, { (ack, message) =>
        GatlingActorSystem.instance.eventStream.publish(UnsubscribeMessage(subscription))
        message
      }))
    }

    def publish(channel: String, data: Any) = {
      ws.sendText(Publish(channel = channel, clientId = Some("${clientId}"), data = data).toJson)
    }

    def sendCommand(channel: String, data: Any) = {
      publish(channel, data)
    }

    def disconnect(disconnect: Disconnect = Disconnect()) = {
      ws.sendText(disconnect.toJson).checkResponse(check(_, { (ack, message) => message}))
    }

    private val check = (message: String, extractor: (Ack, String) => String) => {
      val ack = message.fromJson[List[Ack]].get(0)
      if (ack.successful == true)
        extractor(ack, message)
      else
        ""
    }
  }

  implicit class CometDWsSendActionBuilder(val wsSendActionBuilder: WsSendActionBuilder)(implicit requestTimeOut: FiniteDuration) {
    def checkResponse(fn: String => String, handler: Step2 = wsAwait, saveAs: Option[String] = None) = {
      val response = this.response(fn, handler)
      if (saveAs.isDefined)
        wsSendActionBuilder.check(response.saveAs(saveAs.get))
      else
        wsSendActionBuilder.check(response)
    }

    private def response(fn: String => String, handler: Step2): CheckBuilder[WsCheck, String, String, String] with SaveAs[WsCheck, String, String, String] = {
      handler.within(requestTimeOut).until(1).message.transform(fn).not("")
    }
  }

}

package org.kaloz.gatling.http

import com.typesafe.scalalogging.slf4j.Logging
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

  implicit class WsCometDExtension(val ws: Ws)(implicit requestTimeOut: FiniteDuration) extends Logging {

    import org.kaloz.gatling.json.JsonMarshallableImplicits._

    val cometDProtocolMatchers = Set("\"id\":\"${id}\"", "\"successful\":true")

    def handshake(handshake: Handshake = Handshake()) = {
      ws.sendText(handshake.toJson).checkResponse(fn = { message =>
        val ack = message.fromJson[List[Ack]].get(0)
        ack.clientId.get
      }, matchers = cometDProtocolMatchers + "\"clientId\"", saveAs = Some("clientId"))
    }

    def connect(connect: Connect = Connect()) = {
      ws.sendText(connect.toJson).checkResponse(matchers = cometDProtocolMatchers)
    }

    def subscribe(subscription: String, matchers: Set[String] = Set.empty, subscribeToPubSubProcessor: Boolean = true, extractor: String => Published = { m => m.fromJson[List[PublishedMap]].get(0)}) = {
      ws.sendText(Subscribe(subscription = subscription).toJson).checkResponse(fn = { message =>
        if (subscribeToPubSubProcessor) {
          GatlingActorSystem.instance.eventStream.publish(SubscribeMessage(subscription, matchers, extractor))
        }
        message
      }, matchers = cometDProtocolMatchers)
    }

    def unsubscribe(subscription: String) = {
      ws.sendText(Unsubscribe(subscription = subscription).toJson).checkResponse(fn = { message =>
        GatlingActorSystem.instance.eventStream.publish(UnsubscribeMessage(subscription))
        message
      }, matchers = cometDProtocolMatchers)
    }

    def publish(channel: String, data: Any) = {
      ws.sendText(Publish(channel = channel, data = data).toJson)
    }

    def sendCommand(channel: String, data: Any) = {
      publish(channel, data)
    }

    def disconnect(disconnect: Disconnect = Disconnect()) = {
      ws.sendText(disconnect.toJson).checkResponse(matchers = cometDProtocolMatchers)
    }
  }

  implicit class CometDWsSendActionBuilder(val wsSendActionBuilder: WsSendActionBuilder)(implicit requestTimeOut: FiniteDuration) extends Logging {
    def checkResponse(fn: String => String = { m => m}, matchers: Set[String], saveAs: Option[String] = None) = {
      val response = this.response(fn, matchers)
      wsSendActionBuilder.check(if (saveAs.isDefined)
        response.saveAs(saveAs.get)
      else
        response)
    }

    private def response(fn: String => String, matchers: Set[String]): CheckBuilder[WsCheck, String, CharSequence, String] with SaveAs[WsCheck, String, CharSequence, String] = {
      import org.kaloz.gatling.regex.RegexUtil._
      wsAwait.within(requestTimeOut).until(1).regex(stringToExpression(expression(matchers))).find.transform(fn).exists
    }
  }

}

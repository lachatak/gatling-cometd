package org.kaloz.gatling.http

import com.typesafe.scalalogging.slf4j.Logging
import io.gatling.core.Predef._
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.check._
import io.gatling.core.session._
import io.gatling.core.validation.Validation
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

  def storeValue(key: String, value: Any): Expression[Session] =
    session => session.set(key, value)

  implicit class WsOpenRequestBuilder2CometDBuilder(val wsOpenRequestBuilder: WsOpenRequestBuilder) {
    def registerPubSubProcessor = {
      new CometDOpenRequestBuilder(wsOpenRequestBuilder.commonAttributes, wsOpenRequestBuilder.wsName)
    }
  }

  implicit class WsCometdExtension(val ws: Ws)(implicit requestTimeOut: FiniteDuration) extends Logging {

    import org.kaloz.gatling.json.MarshallableImplicits._

    def handshake(handshake: Handshake = Handshake()) = {
      ws.sendText(handshake.toJson).checkResponse(fn = { message =>
        val ack = message.fromJson[List[Ack]].get(0)
        ack.clientId.getOrElse("")
      }, saveAs = Some("clientId"))
    }

    def connect(connect: Connect = Connect()) = {
      ws.sendText(connect.toJson).checkResponse()
    }

    def subscribe(subscription: String, responsePattern: Option[String] = None, subscribeToPubSubProcessor: Boolean = true, extractor: String => Published = { m => m.fromJson[List[PublishedMap]].get(0)}) = {
      ws.sendText(Subscribe(subscription = subscription).toJson).checkResponse(fn = { message =>
        if (subscribeToPubSubProcessor)
          GatlingActorSystem.instance.eventStream.publish(SubscribeMessage(subscription, responsePattern.getOrElse( s""""channel":"$subscription""""), extractor))
        message
      })
    }

    def unsubscribe(subscription: String) = {
      ws.sendText(Unsubscribe(subscription = subscription).toJson).checkResponse(fn = { message =>
        GatlingActorSystem.instance.eventStream.publish(UnsubscribeMessage(subscription))
        message
      })
    }

    def publish(channel: String, data: Any) = {
      ws.sendText(Publish(channel = channel, data = data).toJson)
    }

    def sendCommand(channel: String, data: Any) = {
      publish(channel, data)
    }

    def disconnect(disconnect: Disconnect = Disconnect()) = {
      ws.sendText(disconnect.toJson).checkResponse()
    }
  }

  implicit class CometDWsSendActionBuilder(val wsSendActionBuilder: WsSendActionBuilder)(implicit requestTimeOut: FiniteDuration) extends Logging {
    def checkResponse(fn: String => String = {m => m}, matchers: List[String] = List("id\":\"${id}", "successful\":true"), saveAs: Option[String] = None) = {
      val response = this.response(fn, matchers)
      if (saveAs.isDefined)
        wsSendActionBuilder.check(response.saveAs(saveAs.get))
      else
        wsSendActionBuilder.check(response)
    }

    private def response(fn: String => String, matchers: List[String]): CheckBuilder[WsCheck, String, CharSequence, String] with SaveAs[WsCheck, String, CharSequence, String] = {
      def generateRegex: (Session) => Validation[String] = { session =>
        val resolvable = "\\$\\{(.*)\\}".r

//        matchers.map { m => resolvable.findFirstIn(m).foreach{ r => session.get(r).as[String] }}.mkString("(?=.*\\b", "\\b)(?=.*\\b", "\\b).*")
        session.get("id")
        logger.info(matchers.map { m => resolvable.findFirstIn(m).foreach{ r => session.get(r).as[String] }}.mkString("(?=.*\\b", "\\b)(?=.*\\b", "\\b).*"))
        matchers.mkString("(?=.*\\b", "\\b)(?=.*\\b", "\\b).*")
      }
      wsAwait.within(requestTimeOut).until(1).regex(generateRegex).find.transform(fn).exists
    }
  }

}

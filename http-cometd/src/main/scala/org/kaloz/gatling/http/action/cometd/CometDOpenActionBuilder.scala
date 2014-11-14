package org.kaloz.gatling.http.action.cometd

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.Predef._
import io.gatling.core.check.SaveAs
import io.gatling.core.config.Protocols
import io.gatling.core.session._
import io.gatling.http.Predef._
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.action.ws._
import io.gatling.http.check.ws._
import org.kaloz.gatling.http.action.cometd.CheckBuilderConverter._
import org.kaloz.gatling.http.action.cometd.MessageConverter._
import org.kaloz.gatling.http.cometd.CometDMessages._
import org.kaloz.gatling.http.request.builder.cometd.CometDOpenRequestBuilder
import org.kaloz.gatling.json.JsonMarshallableImplicits._
import org.kaloz.gatling.regex.RegexUtil._

import scala.concurrent.duration.FiniteDuration

class CometDOpenActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: CometDOpenRequestBuilder) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)
    actor(new CometDOpenAction(requestName, wsName, request, next, protocol, requestBuilder.pubSubProcessorManifest))
  }
}

class CometDHandshakeActionBuilder(requestName: Expression[String], cometDName: String, handshake: Handshake)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, handshake, CometDCheckBuilder(cometDProtocolMatchers + "\"clientId\"", { message =>
    val ack = message.fromJson[List[Ack]].head
    ack.clientId.get
  }, Some("clientId")), next))
}

class CometDConnectActionBuilder(requestName: Expression[String], cometDName: String, connect: Connect)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, connect, CometDCheckBuilder(), next))
}

class CometDDisconnectActionBuilder(requestName: Expression[String], cometDName: String, disconnect: Disconnect)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, disconnect, CometDCheckBuilder(), next))
}

class CometDPublishActionBuilderStep1(requestName: Expression[String], cometDName: String, publish: Publish)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def acceptResponseContains(matchers: Set[String]) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, publish, None, next))
}

class CometDPublishActionBuilderStep2(requestName: Expression[String], cometDName: String, publish: Publish, matchers: Set[String], fn: String => String = identity[String], saveAs: Option[String] = None)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def transformer(transformer: String => String) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers, fn, saveAs)

  def saveAs(saveAs: String) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers, fn, Some(saveAs))

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, publish, CometDCheckBuilder(matchers, fn, saveAs), next))
}

class CometDSubscribeActionBuilderStep1(requestName: Expression[String], cometDName: String, subscribe: Subscribe)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def acceptPushContains(matchers: Set[String]) = new CometDSubscribeActionBuilderStep2(requestName, cometDName, subscribe, matchers)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDSubscribeAction(requestName, cometDName, subscribe, Set.empty, None, next))
}

class CometDSubscribeActionBuilderStep2(requestName: Expression[String], cometDName: String, subscribe: Subscribe, matchers: Set[String], extractor: String => Published = { m => m.fromJson[List[PublishedMap]].head})(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def extractor(extractor: String => Published) = new CometDSubscribeActionBuilderStep2(requestName, cometDName, subscribe, matchers, extractor)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDSubscribeAction(requestName, cometDName, subscribe, matchers, Some(extractor), next))
}

class CometDUnsubscribeActionBuilder(requestName: Expression[String], cometDName: String, unsubscribe: Unsubscribe)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDUnsubscribeAction(requestName, cometDName, unsubscribe, next))
}

case class CometDCheckBuilder(matchers: Set[String] = cometDProtocolMatchers, transformer: String => String = identity[String], saveAsName: Option[String] = None)(implicit requestTimeOut: FiniteDuration) {

  val wsCheckBuilder = {
    val wsCheckBuilder = this.buildWsCheckBuilder(transformer, matchers)
    if (saveAsName.isDefined)
      wsCheckBuilder.saveAs(saveAsName.get)
    else
      wsCheckBuilder
  }

  private def buildWsCheckBuilder(fn: String => String, matchers: Set[String])(implicit requestTimeOut: FiniteDuration): WsCheckBuilder with SaveAs[WsCheck, String, _, String] = {
    wsAwait.within(requestTimeOut).until(1).regex(stringToExpression(expression(matchers))).find.transform(fn).exists
  }
}

object CheckBuilderConverter {

  implicit def toWsCheckBuilder(cometDCheckBuilder: CometDCheckBuilder): Option[WsCheckBuilder] = Some(cometDCheckBuilder.wsCheckBuilder)

  implicit def toWsCheck(cometDCheckBuilder: CometDCheckBuilder): Option[WsCheck] = Some(cometDCheckBuilder.wsCheckBuilder.build)
}

object MessageConverter {

  implicit def toTextMessage(data: Any): Expression[WsMessage] = stringToExpression(data.toJson).map(TextMessage)
}
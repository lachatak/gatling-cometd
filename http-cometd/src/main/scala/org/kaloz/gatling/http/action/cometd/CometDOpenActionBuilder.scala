package org.kaloz.gatling.http.action.cometd

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.Predef._
import io.gatling.core.check.{CheckBuilder, SaveAs}
import io.gatling.core.config.Protocols
import io.gatling.core.session._
import io.gatling.http.Predef._
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.action.ws._
import io.gatling.http.check.ws.WsCheck
import org.kaloz.gatling.http.cometd.CometDMessages._
import org.kaloz.gatling.http.request.builder.cometd.CometDOpenRequestBuilder

import scala.concurrent.duration.FiniteDuration


class CometDOpenActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: CometDOpenRequestBuilder) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)
    actor(new CometDOpenAction(requestName, wsName, request, next, protocol, requestBuilder.pubSubProcessorManifest))
  }
}

import org.kaloz.gatling.http.action.cometd.MessageConverter._
import org.kaloz.gatling.json.JsonMarshallableImplicits._

class CometDHandshakeActionBuilder(requestName: Expression[String], cometDName: String, handshake: Handshake)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, handshake, buildCheckResponse(cometDProtocolMatchers + "\"clientId\"", { message =>
    val ack = message.fromJson[List[Ack]].head
    ack.clientId.get
  }, Some("clientId")), next))
}

class CometDConnectActionBuilder(requestName: Expression[String], cometDName: String, connect: Connect)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, connect, buildCheckResponse(cometDProtocolMatchers), next))
}

class CometDDisconnectActionBuilder(requestName: Expression[String], cometDName: String, disconnect: Disconnect)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, disconnect, buildCheckResponse(cometDProtocolMatchers), next))
}

class CometDPublishActionBuilderStep1(requestName: Expression[String], cometDName: String, publish: Publish)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder {

  def acceptResponseContains(matchers: Set[String]) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, publish, None, next))
}

class CometDPublishActionBuilderStep2(requestName: Expression[String], cometDName: String, publish: Publish, matchers: Set[String], fn: String => Any = { m => m}, saveAs: Option[String] = None)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def transformer(transformer: String => Any) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers, fn, saveAs)

  def saveAs(saveAs: Option[String]) = new CometDPublishActionBuilderStep2(requestName, cometDName, publish, matchers, fn, saveAs)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new WsSendAction(requestName, cometDName, publish, buildCheckResponse(matchers, fn, saveAs), next))
}

class CometDSubscribeActionBuilderStep1(requestName: Expression[String], cometDName: String, subscribe: Subscribe)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def acceptPushContains(matchers: Set[String]) = new CometDSubscribeActionBuilderStep2(requestName, cometDName, subscribe, matchers)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDSubscribeAction(requestName, cometDName, subscribe, Set.empty, None, next))
}

class CometDSubscribeActionBuilderStep2(requestName: Expression[String], cometDName: String, subscribe: Subscribe, matchers: Set[String], extractor: String => Published = { m => m.fromJson[List[PublishedMap]].head})(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def extractor(extractor: String => Published) = new CometDSubscribeActionBuilderStep2(requestName, cometDName, subscribe, matchers, extractor)

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDSubscribeAction(requestName, cometDName, subscribe, matchers, Some(extractor), next))
}

class CometDUnsubscribeActionBuilder(requestName: Expression[String], cometDName: String, unsubscribe: Unsubscribe)(implicit requestTimeOut: FiniteDuration) extends HttpActionBuilder with CometDCheckBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new CometDUnsubscribeAction(requestName, cometDName, unsubscribe, next))
}

trait CometDCheckBuilder {

  val cometDProtocolMatchers = Set("\"id\":\"${id}\"", "\"successful\":true")

  protected def buildCheckResponse(matchers: Set[String], transformer: String => Any = { m => m}, saveAs: Option[String] = None)(implicit requestTimeOut: FiniteDuration) = {
    val response = this.response(transformer, matchers)
    Some(if (saveAs.isDefined)
      response.saveAs(saveAs.get).build
    else
      response.build)
  }

  protected def response(fn: String => Any, matchers: Set[String])(implicit requestTimeOut: FiniteDuration): CheckBuilder[WsCheck, String, CharSequence, Any] with SaveAs[WsCheck, String, CharSequence, Any] = {
    import org.kaloz.gatling.regex.RegexUtil._
    wsAwait.within(requestTimeOut).until(1).regex(stringToExpression(expression(matchers))).find.transform(fn).exists
  }
}

object MessageConverter {

  import org.kaloz.gatling.json.JsonMarshallableImplicits._

  implicit def toTextMessage(data: Any): Expression[WsMessage] = stringToExpression(data.toJson).map(TextMessage)
}



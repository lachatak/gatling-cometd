package org.kaloz.gatling.http.action.cometd

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.config.Protocols
import io.gatling.core.session._
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.ws.WsCheck
import org.kaloz.gatling.http.request.builder.cometd.CometDOpenRequestBuilder


class CometDOpenActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: CometDOpenRequestBuilder, check: Option[WsCheck] = None) extends HttpActionBuilder {

  def check(check: WsCheck) = new CometDOpenActionBuilder(requestName, wsName, requestBuilder, Some(check))

  def build(next: ActorRef, protocols: Protocols): ActorRef = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)
    actor(new CometDOpenAction(requestName, wsName, request, check, next, protocol, requestBuilder.pubSubProcessorManifest))
  }
}

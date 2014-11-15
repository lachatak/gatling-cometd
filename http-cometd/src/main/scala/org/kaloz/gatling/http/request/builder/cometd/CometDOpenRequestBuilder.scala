package org.kaloz.gatling.http.request.builder.cometd

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._
import io.gatling.http.Predef._
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.ws.WsRequestExpressionBuilder
import io.gatling.http.request.builder.{CommonAttributes, RequestBuilder}
import org.kaloz.gatling.http.action.cometd.{CometDOpenActionBuilder, PushProcessorActor}

import scala.reflect.Manifest

object CometDOpenRequestBuilder {

  implicit def toActionBuilder(requestBuilder: CometDOpenRequestBuilder): ActionBuilder = new CometDOpenActionBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.wsName, requestBuilder)
}

class CometDOpenRequestBuilder(commonAttributes: CommonAttributes, val wsName: String, val pushProcessorManifest: Option[Manifest[_]] = None) extends RequestBuilder[CometDOpenRequestBuilder](commonAttributes) {

  def newInstance(commonAttributes: CommonAttributes) = new CometDOpenRequestBuilder(commonAttributes, wsName, pushProcessorManifest)

  def pushProcessor[T <: PushProcessorActor : Manifest] = new CometDOpenRequestBuilder(commonAttributes, wsName, Some(manifest))

  def build(protocol: HttpProtocol): Expression[Request] = new WsRequestExpressionBuilder(commonAttributes, protocol).build
}

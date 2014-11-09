package org.kaloz.gatling.http.cometd

import io.gatling.core.session._
import org.kaloz.gatling.http.request.builder.cometd.CometD

import scala.concurrent.duration.FiniteDuration

object Predef {

  def cometD(requestName: Expression[String])(implicit requestTimeOut: FiniteDuration) = new CometD(requestName)
}

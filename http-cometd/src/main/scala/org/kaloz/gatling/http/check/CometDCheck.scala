package org.kaloz.gatling.http.check

import io.gatling.core.Predef._
import io.gatling.core.check.SaveAs
import io.gatling.http.Predef._
import io.gatling.http.check.ws._
import org.kaloz.gatling.http.action.cometd._
import org.kaloz.gatling.regex.RegexUtil._

import scala.concurrent.duration.FiniteDuration

case class CometDCheck(matchers: Set[String] = cometDProtocolMatchers, transformer: String => String = identity[String], saveAsName: Option[String] = None)(implicit requestTimeOut: FiniteDuration) {

  val wsCheckBuilder = {
    if (saveAsName.isDefined)
      _wsCheckBuilder.saveAs(saveAsName.get)
    else
      _wsCheckBuilder
  }

  lazy val _wsCheckBuilder: WsCheckBuilder with SaveAs[WsCheck, String, _, String] = {
    wsAwait.within(requestTimeOut).until(1).regex(stringToExpression(containsAll(matchers))).find.transform(transformer).exists
  }
}

object CheckBuilderConverter {

  implicit def toWsCheckBuilder(cometDCheck: CometDCheck): Option[WsCheckBuilder] = Some(cometDCheck.wsCheckBuilder)

  implicit def toWsCheck(cometDCheck: CometDCheck): Option[WsCheck] = Some(cometDCheck.wsCheckBuilder.build)
}

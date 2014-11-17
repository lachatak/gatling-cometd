package org.kaloz.gatling.http.action

package object cometd {

  val cometDProtocolMatchers = Set("\"id\":\"${cometDMessageId}\"", "\"successful\":true")

}

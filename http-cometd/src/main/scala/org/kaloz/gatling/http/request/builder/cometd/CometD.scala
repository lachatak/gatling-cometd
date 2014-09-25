package org.kaloz.gatling.http.request.builder.cometd

import io.gatling.core.session._
import io.gatling.http.action.ws._
import io.gatling.http.request.builder.CommonAttributes
import io.gatling.http.request.builder.ws.Ws

/**
 * @param requestName The name of this request
 * @param wsName The name of the session attribute used to store the websocket
 */
class CometD(val requestName: Expression[String], val wsName: String = Ws.DefaultWebSocketName) {

  def wsName(wsName: String) = new CometD(requestName, wsName)

  /**
   * Opens a cometD connection using web socket and stores it in the session.
   *
   * @param url The socket URL
   *
   */
  def open(url: Expression[String]) = new CometDOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), wsName)

  /**
   * Sends a text message on the given websocket.
   *
   * @param text The message
   */
  def sendText(text: Expression[String]) = new WsSendActionBuilder(requestName, wsName, text.map(TextMessage))

  /**
   * Reconciliate the main state with the one of the websocket flow.
   */
  def reconciliate = new WsReconciliateActionBuilder(requestName, wsName)

  /**
   * Closes a websocket.
   */
  def close = new WsCloseActionBuilder(requestName, wsName)

}

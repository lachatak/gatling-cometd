package org.kaloz.gatling.http.action.cometd

import com.ning.http.client.ws.WebSocket
import io.gatling.http.action.ws._
import io.gatling.http.ahc.WsTx
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.{Message, SessionUpdates}

import scala.reflect.ClassTag

class CometDActor(wsName: String, pushProcessorManifest: Option[ClassTag[_]] = None) extends WsActor(wsName) {

  type ReceiveDecorator = PartialFunction[Any, Any]

  val pushProcessor = pushProcessorManifest.map(manifest => context.actorOf(PushProcessorActor.props(manifest)))

  override def receive = onOpen andThen initialState

  val onOpen: ReceiveDecorator = {

    case OnOpen(tx, webSocket, end) =>
      val newSession = tx.session.set(PushProcessorActor.PushProcessorName, pushProcessor)
      val newTx = tx.copy(session = newSession)

      OnOpen(newTx, webSocket, end)

    case m => m
  }

  def onTextMessage(webSocket: WebSocket, tx: WsTx): ReceiveDecorator = {

    case m@OnTextMessage(message, _) =>
      pushProcessor.foreach(_ ! Message(message))
      m

    case SessionUpdates(newUpdates) =>
      val newTx = tx.copy(updates = newUpdates ::: tx.updates)
      context.become(openState(webSocket, newTx))

    case m => m

  }

  override def openState(webSocket: WebSocket, tx: WsTx): Receive = onTextMessage(webSocket, tx) andThen super.openState(webSocket, tx)
}
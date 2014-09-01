package org.kaloz.gatling.http.action.cometd

import com.ning.http.client.websocket.WebSocket
import io.gatling.core.check.CheckResult
import io.gatling.core.result.message.{OK, Status}
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper._
import io.gatling.core.validation.Success
import io.gatling.http.action.ws._
import io.gatling.http.ahc.WsTx
import io.gatling.http.check.ws.UntilCount

import scala.collection.mutable

class CometDActor(wsName: String) extends WsActor(wsName) {

  private def logRequest(session: Session, requestName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String]): Unit = {
    writeRequestData(
      session,
      requestName,
      started,
      ended,
      ended,
      ended,
      status,
      errorMessage)
  }

  private def onTextMessage(webSocket: WebSocket, tx: WsTx): Receive = {

    def succeedPendingCheck(results: List[CheckResult]): Unit = {
      tx.check match {
        case Some(check) =>
          // expected count met, let's stop the check
          logRequest(tx.session, tx.requestName, OK, tx.start, nowMillis, None)

          val checkResults = results.filter(_.hasUpdate)

          val newUpdates = checkResults match {
            case Nil =>
              // nothing to save, no update
              tx.updates

            case List(checkResult) =>
              // one single value to save
              checkResult.update.getOrElse(Session.Identity) :: tx.updates

            case _ =>
              // multiple values, let's pile them up
              val mergedCaptures = checkResults
                .collect { case CheckResult(Some(value), Some(saveAs)) => saveAs -> value}
                .groupBy(_._1)
                .mapValues(_.flatMap(_._2 match {
                case s: Seq[Any] => s
                case v => Seq(v)
              }))

              val newUpdate = (session: Session) => session.setAll(mergedCaptures)
              newUpdate :: tx.updates
          }

          if (check.blocking) {
            // apply updates and release blocked flow
            val newSession = tx.session.update(newUpdates)

            tx.next ! newSession
            val newTx = tx.copy(session = newSession, updates = Nil, check = None, pendingCheckSuccesses = Nil)
            context.become(openState(webSocket, newTx))

          } else {
            // add to pending updates
            val newTx = tx.copy(updates = newUpdates, check = None, pendingCheckSuccesses = Nil)
            context.become(openState(webSocket, newTx))
          }

        case _ =>
      }
    }

    {
      case OnTextMessage(message, time) =>
        logger.debug(s"Received text message on websocket '$wsName':$message")

        context.system.eventStream.publish(Message(message))

        implicit val cache = mutable.Map.empty[Any, Any]

        tx.check.foreach { check =>

          check.check(message, tx.session) match {
            case Success(result) =>
              val results = result :: tx.pendingCheckSuccesses

              check.expectation match {
                case UntilCount(count) if count == results.length => succeedPendingCheck(results)

                case _ =>
                  // let's pile up
                  val newTx = tx.copy(pendingCheckSuccesses = results)
                  context.become(openState(webSocket, newTx))
              }

            case _ =>
          }
        }
    }
  }

  override def openState(webSocket: WebSocket, tx: WsTx): Receive = onTextMessage(webSocket, tx) orElse super.openState(webSocket, tx)

}

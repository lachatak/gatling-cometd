package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.gatling.core.session.Session
import org.kaloz.gatling.http.action.cometd.SessionHandler.{Forward, Store}

object SessionHandler {

  case class Store(data: Map[String, Any])

  case class Forward(next: ActorRef, session: Session)

}

class SessionHandler extends Actor with ActorLogging {

  def receive = handleSession()

  def handleSession(storedData: Map[String, Any] = Map.empty): Receive = {
    case Forward(next, session) =>
      val newSession = session.setAll(storedData)
      next ! newSession
      context become handleSession()
    case Store(data) =>
      context become handleSession(storedData ++ data)
  }
}
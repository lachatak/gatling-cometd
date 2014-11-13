package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging, Props}
import io.gatling.core.session.{Session, SessionPrivateAttributes}
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.SessionUpdates
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import org.kaloz.gatling.regex.RegexUtil._

import scala.reflect.ClassTag
import scala.util.matching.Regex

abstract class PushProcessorActor extends Actor with ActorLogging {

  override def receive: Actor.Receive = process()

  def process(subscriptions: Map[String, (Regex, String => Published)] = Map.empty): Actor.Receive = {
    case SubscribeMessage(subscription, matchers, extractor) =>
      context become process(subscriptions + (subscription ->(expression(matchers).r, extractor)))
    case UnsubscribeMessage(subscription) =>
      context become process(subscriptions = subscriptions - subscription)
    case Message(message) =>
      for {
        (regex, extractor) <- subscriptions.values
        matching <- regex.findFirstIn(message)
        result = extractor(matching)
      } yield context.parent ! SessionUpdates(messageReceive(result).map { case (key, value) => (session: Session) => session.set(key, value)}(collection.breakOut): List[Session => Session])
  }

  def messageReceive: PartialFunction[Published, Map[String, Any]]
}

object PushProcessorActor {

  val PushProcessorName = SessionPrivateAttributes.PrivateAttributePrefix + "http.pushProcessor"

  def props(pushProcessorManifest: ClassTag[_]): Props = Props.create(pushProcessorManifest.runtimeClass)

  case class SubscribeMessage(subscription: String, matchers: Set[String], extractor: String => Published)

  case class UnsubscribeMessage(subscription: String)

  case class Message(message: String)

  case class SessionUpdates(update: List[Session => Session])

}

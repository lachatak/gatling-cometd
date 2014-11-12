package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.gatling.core.session.SessionPrivateAttributes
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.{Message, SubscribeMessage, UnsubscribeMessage}
import org.kaloz.gatling.http.action.cometd.SessionHandler.Store
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import org.kaloz.gatling.regex.RegexUtil._

import scala.reflect.ClassTag
import scala.util.matching.Regex

abstract class PushProcessorActor(sessionHandler: ActorRef) extends Actor with ActorLogging {

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
      } yield sessionHandler ! Store(messageReceive(result))
  }

  def messageReceive: PartialFunction[Published, Map[String, Any]]
}

class DefaultPushProcessor(sessionHandler: ActorRef) extends PushProcessorActor(sessionHandler) {

  override def receive: Actor.Receive = {
    case _ =>
  }

  def messageReceive = {
    case _ => Map.empty
  }
}

object PushProcessorActor {

  val PushProcessorName = SessionPrivateAttributes.PrivateAttributePrefix + "http.pushProcessor"

  def props(pushProcessorManifest: Option[ClassTag[_]], sessionHandler: ActorRef): Props = Props.create(pushProcessorManifest.getOrElse(Manifest.classType(classOf[DefaultPushProcessor])).runtimeClass, sessionHandler)

  case class SubscribeMessage(subscription: String, matchers: Set[String], extractor: String => Published)

  case class UnsubscribeMessage(subscription: String)

  case class Message(message: String)

}

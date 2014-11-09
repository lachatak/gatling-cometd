package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.gatling.core.session.SessionPrivateAttributes
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.{Message, SubscribeMessage, UnsubscribeMessage}
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import org.kaloz.gatling.regex.RegexUtil._

import scala.reflect.ClassTag
import scala.util.matching.Regex

abstract class PushProcessorActor extends Actor with ActorLogging {

  override def receive: Actor.Receive = process()

  def process(subscriptions: Map[String, (Regex, String => Published)] = Map.empty): Actor.Receive = {
    case SubscribeMessage(subscription, matchers, extractor) =>
      //      log.info(s"Add subscription $subscription")
      context become process(subscriptions + (subscription ->(expression(matchers).r, extractor)))
    case UnsubscribeMessage(subscription) =>
      //      log.info(s"Remove subscription $subscription")
      context become process(subscriptions = subscriptions - subscription)
    case Message(message) =>
      //      log.info(s"OnMessage $message")
      //      log.info(s"Existing subscriptions $subscriptions")
      for {
        (regex, extractor) <- subscriptions.values
        matching <- regex.findFirstIn(message)
        result = extractor(matching)
      } yield messageReceive(result)
  }

  def messageReceive: PartialFunction[Published, Unit]
}

class DefaultPushProcessor extends PushProcessorActor {

  override def receive: Actor.Receive = {
    case _ =>
  }

  def messageReceive = {
    case _ =>
  }
}

object PushProcessorActor {

  val PushProcessorName = SessionPrivateAttributes.PrivateAttributePrefix + "http.pushProcessor"

  def props(pushProcessorManifest: Option[ClassTag[_]], sessionHandler: ActorRef): Props = Props.create(pushProcessorManifest.getOrElse(Manifest.classType(classOf[DefaultPushProcessor])).runtimeClass, sessionHandler)

  case class SubscribeMessage(subscription: String, matchers: Set[String], extractor: String => Published)

  case class UnsubscribeMessage(subscription: String)

  case class Message(message: String)

}

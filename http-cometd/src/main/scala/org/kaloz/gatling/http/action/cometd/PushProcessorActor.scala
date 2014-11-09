package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging}
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import org.kaloz.gatling.regex.RegexUtil._

import scala.util.matching.Regex

case class SubscribeMessage(subscription: String, matchers: Set[String], extractor: String => Published)

case class UnsubscribeMessage(subscription: String)

case class Message(message: String)

abstract class PushProcessorActor extends Actor with ActorLogging {

  override def receive: Actor.Receive = process()

  def process(subscriptions: Map[String, (Regex, String => Published)] = Map.empty): Actor.Receive = {
    case SubscribeMessage(subscription, matchers, extractor) =>
      log.info(s"Add subscription $subscription")
      context become process(subscriptions + (subscription ->(expression(matchers).r, extractor)))
    case UnsubscribeMessage(subscription) =>
      log.info(s"Remove subscription $subscription")
      context become process(subscriptions = subscriptions - subscription)
    case Message(message) =>
      log.info(s"OnMessage $message")
      log.info(s"Existing subscriptions $subscriptions")
      for {
        (regex, extractor) <- subscriptions.values
        matching <- regex.findFirstIn(message)
        result = extractor(matching)
      } yield messageReceive(result)
  }

  def messageReceive: PartialFunction[Published, Unit]
}

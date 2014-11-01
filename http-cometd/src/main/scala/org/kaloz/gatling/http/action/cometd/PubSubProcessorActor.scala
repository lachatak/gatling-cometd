package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging}
import io.gatling.core.session._
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import org.kaloz.gatling.regex.RegexUtil._

import scala.util.matching.Regex

case class SubscribeMessage(subscription: String, matchers: Set[String], extractor: String => Published)

case class UnsubscribeMessage(subscription: String)

case class Message(message: String)

abstract class PubSubProcessorActor extends Actor with ActorLogging {

  var subscriptions: Map[String, (Regex, String => Published)] = Map.empty

  override def preStart = {
    context.system.eventStream.subscribe(context.self, classOf[SubscribeMessage])
    context.system.eventStream.subscribe(context.self, classOf[UnsubscribeMessage])
//    context.system.eventStream.subscribe(context.self, classOf[Message])
  }

  override def receive: Actor.Receive = pubSubReceive orElse messageReceive

  def pubSubReceive: Actor.Receive = {
    case SubscribeMessage(subscription, matchers, extractor) =>
      subscriptions = subscriptions + (subscription ->(expression(matchers).r, extractor))
    case UnsubscribeMessage(subscription) =>
      subscriptions = subscriptions - subscription
    case Message(message) =>
      log.info(s"PubSub $message")
      log.info(s"subscriptions $subscriptions")
      for {
        (regex, extractor) <- subscriptions.values
        matching <- regex.findFirstIn(message)
        result = extractor(matching)
      } yield self ! result
  }

  def messageReceive: Actor.Receive
}

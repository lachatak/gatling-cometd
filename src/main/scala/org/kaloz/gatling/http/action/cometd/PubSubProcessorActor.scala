package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging}
import org.kaloz.gatling.http.cometd.CometDMessages.Published

import scala.util.matching.Regex

case class SubscribeMessage(subscription: String, responsePattern: String, extractor: String => Published)

case class UnsubscribeMessage(subscription: String)

case class Message(message: String)

trait PubSubProcessorActor extends Actor with ActorLogging {

  var subscriptions: Map[String, (Regex, String => Published)] = Map.empty

  override def preStart = {
    context.system.eventStream.subscribe(context.self, classOf[SubscribeMessage])
    context.system.eventStream.subscribe(context.self, classOf[UnsubscribeMessage])
    context.system.eventStream.subscribe(context.self, classOf[Message])
  }

  override def receive: Actor.Receive = pubSubReceive orElse messageReceive

  def pubSubReceive: Actor.Receive = {
    case SubscribeMessage(subscription, responsePattern, extractor) =>
      subscriptions = subscriptions + (subscription ->(responsePattern.r, extractor))
    case UnsubscribeMessage(subscription) =>
      subscriptions = subscriptions - subscription
    case Message(message) =>

      subscriptions.values.foreach { value =>
        value._1.findFirstIn(message).foreach { m =>
          self ! value._2(message)
        }
      }
  }

  def messageReceive: Actor.Receive
}

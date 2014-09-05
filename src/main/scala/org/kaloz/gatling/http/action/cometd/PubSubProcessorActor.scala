package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging}
import org.kaloz.gatling.http.cometd.CometDMessages.Published
import io.gatling.core.Predef._
import io.gatling.core.session._

import scala.util.matching.Regex
import org.kaloz.gatling.regex.RegexUtil._

case class SubscribeMessage(subscription: String, matchers: List[String], extractor: String => Published)

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
    case SubscribeMessage(subscription, matchers, extractor) =>
      subscriptions = subscriptions + (subscription ->(expression(matchers).r, extractor))
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

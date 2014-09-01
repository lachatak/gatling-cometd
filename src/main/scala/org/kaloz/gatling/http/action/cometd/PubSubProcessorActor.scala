package org.kaloz.gatling.http.action.cometd

import akka.actor.{Actor, ActorLogging}
import io.gatling.core.Predef._
import org.kaloz.gatling.http.cometd.CometDMessages.Published

import scala.util.matching.Regex

case class SubscribeMessage(subscription: String, responsePattern: String)

case class UnsubscribeMessage(subscription: String)

case class Message(message: String)

trait PubSubProcessorActor extends Actor with ActorLogging {

  var subscriptions: Map[String, Regex] = Map.empty

  override def preStart = {
    context.system.eventStream.subscribe(context.self, classOf[SubscribeMessage])
    context.system.eventStream.subscribe(context.self, classOf[UnsubscribeMessage])
    context.system.eventStream.subscribe(context.self, classOf[Message])
  }

  override def receive: Actor.Receive = pubSubReceive orElse messageReceive

  def pubSubReceive: Actor.Receive = {
    case SubscribeMessage(subscription, responsePattern) =>
      subscriptions = subscriptions + (subscription -> responsePattern.r)
    case UnsubscribeMessage(subscription) =>
      subscriptions = subscriptions - subscription
    case Message(message) =>
      import org.kaloz.gatling.json.MarshallableImplicits._

      subscriptions.values.foreach { responsePattern =>
        responsePattern.findFirstIn(message).foreach { m =>
          self ! message.fromJson[List[Published]].get(0)
        }
      }
  }

  def messageReceive: Actor.Receive
}

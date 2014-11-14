/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaloz.gatling.http.action.cometd

import akka.actor.ActorRef
import io.gatling.core.session._
import io.gatling.core.validation.Validation
import io.gatling.http.action.RequestAction
import io.gatling.http.action.ws._
import org.kaloz.gatling.http.action.cometd.CheckBuilderConverter._
import org.kaloz.gatling.http.action.cometd.PushProcessorActor.SubscribeMessage
import org.kaloz.gatling.http.cometd.CometDMessages._
import org.kaloz.gatling.json.JsonMarshallableImplicits._

import scala.concurrent.duration.FiniteDuration

class CometDSubscribeAction(val requestName: Expression[String], cometDName: String, message: Expression[WsMessage], matchers: Set[String], extractor: Option[String => Published], val next: ActorRef)(implicit requestTimeOut: FiniteDuration) extends RequestAction with WsAction {

  def sendRequest(requestName: String, session: Session): Validation[Unit] = {
    for {
      cometDActor <- fetchWebSocket(cometDName, session)
      resolvedMessage <- message(session)
    } yield cometDActor ! Send(requestName, resolvedMessage, CometDCheckBuilder(cometDProtocolMatchers, generateTransformer(session)), next, session)
  }

  def generateTransformer(session: Session): String => String = {
    message =>
      val ack = message.fromJson[List[Ack]].head
      for {
        s <- ack.subscription if (ack.successful && matchers.nonEmpty)
        e <- extractor
        pushProcessor <- session.attributes.get(PushProcessorActor.PushProcessorName)
        actorRef <- pushProcessor.asInstanceOf[Option[ActorRef]]
      } yield actorRef ! SubscribeMessage(s, matchers, e)
      message
  }
}

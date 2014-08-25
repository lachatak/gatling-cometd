import akka.actor.{Actor, Props}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.check.{CheckBuilder, SaveAs}
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._
import io.gatling.http.action.ws.WsSendActionBuilder
import io.gatling.http.check.ws.WsCheck

import scala.concurrent.duration._

//case class Request(clientId: String = "${clientId}", channel: String, data: Map[String, String])
//case class Subscription(id:String="2",channel: String = "/meta/subscribe", subscription:String)


case class Handshake(id: String = "0", channel: String = "/meta/handshake", supportedConnectionTypes: List[String] = List("long-polling"), version: String = "1.0")

case class Connect(id: String = "1", channel: String = "/meta/connect", clientId: String = "${clientId}", connectionType: String = "websocket", advice: Map[String, String] = Map("timeout" -> "0"))

case class Response(id: String, channel: String, clientId: String, successful: Option[Boolean])

class GatlingTest extends Simulation {

  import CometDExtension._
  import MarshallableImplicits._

  val httpConf = http
    .wsBaseURL("ws://localhost:8000")
    .disableFollowRedirect.disableWarmUp

  val sessionDataActor = GatlingActorSystem.instance.actorOf(SessionDataContainer.props)

  val scn = scenario("WebSocket")
    .exec(ws("Connect WS").open("/beyaux"))
    .pause(1 second)
    .exec(ws("Handshake").sendText(Handshake().toJson).handleResponse(clientIdExtractor, saveAs = Some("clientId")))
    .exec(ws("Connect").sendText(Connect().toJson).check(wsAwait.within(5 seconds).until(1).message))
    .exec(ws("Close WS").close)

  setUp(
    scn.inject(
      rampUsers(1) over (1 seconds)
    ).protocols(httpConf)
  )

  val clientIdExtractor = (response: Response) => {
    if (response.successful.isDefined && response.successful.get == true && response.id == "0")
      response.clientId
    else
      ""
  }
}

object GatlingTest extends App {
  val gatlingPropertyBuilder = new GatlingPropertiesBuilder
  gatlingPropertyBuilder.simulationClass(classOf[GatlingTest].getName)
  Gatling.fromMap(gatlingPropertyBuilder.build)
}

object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def fromJson[T](json: String)(implicit m: Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}

object MarshallableImplicits {

  implicit class Unmarshallable(unMarshallMe: String) {
    def fromJson[T]()(implicit m: Manifest[T]): T = JsonUtil.fromJson[T](unMarshallMe)
  }

  implicit class Marshallable[T](marshallMe: T) {
    def toJson: String = JsonUtil.toJson(marshallMe)
  }

}

object CometDExtension {

  implicit class WsSendActionBuilderExtension(wsSendActionBuilder: WsSendActionBuilder) {
    def handleResponse(fn: => Response => String, handler: Step2 = wsAwait, saveAs: Option[String] = None) = {
      val response = response2(fn, handler)
      if (saveAs.isDefined)
        wsSendActionBuilder.check(response.saveAs(saveAs.get))
      else
        wsSendActionBuilder.check(response)
    }

    private def response2(fn: => Response => String, handler: Step2): CheckBuilder[WsCheck, String, String, String] with SaveAs[WsCheck, String, String, String] = {
      import MarshallableImplicits._
      handler.within(5 seconds).until(1).message.transform(message => message.fromJson[List[Response]].get(0)).transform(fn(_)).not("")
    }
  }

}

class SessionDataContainer extends Actor {

  context.become(handle())

  override def receive: Actor.Receive = {
    case _ => throw new RuntimeException()
  }

  def handle(container: Map[String, Map[String, String]] = Map.empty): Receive = {
    case Save(sessionId, key, value) => save(container, sessionId, key, value)
    case Get(sessionId, key) => sender ! Value(sessionId, key, getSessionMap(container, sessionId).get(key))
    case GetGeneratedId(sessionId) => {
      val id = (Integer.parseInt(getSessionMap(container, sessionId).getOrElse("id", "0")) + 1).toString
      save(container, sessionId, "id", id)
      sender ! Value(sessionId, "id", Some(id))
    }
  }

  private def save(container: Map[String, Map[String, String]], sessionId: String, key: String, value: String) {
    context become (handle(container + (sessionId -> (getSessionMap(container, sessionId) + (key -> value)))))
  }

  private def getSessionMap(container: Map[String, Map[String, String]], sessionId: String) = {
    container.getOrElse(sessionId, Map.empty[String, String])
  }
}

object SessionDataContainer {
  def props = Props(classOf[SessionDataContainer])

}

case class Save(sessionId: String, key: String, value: String)

case class Get(sessionId: String, key: String)

case class GetGeneratedId(sessionId: String)

case class Value(sessionId: String, key: String, value: Option[String])



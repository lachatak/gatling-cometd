import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._

sealed trait Message

case class Request(request: String) extends Message

case class Response(response: String) extends Message

class GatlingTest extends Simulation {

  import MarshallableImplicits._

  val httpConf = http
    .wsBaseURL("ws://localhost:8000")
    .disableFollowRedirect.disableWarmUp

  val scn = scenario("WebSocket")
    .exec(ws("Connect WS").open("/"))
    .pause(1 second)
    .exec(ws("Echo").sendText(Request("Please echo!").toJson).check(wsAwait.within(5 seconds).until(1).message.transform(m => m.fromJson[Response].response).is("Please echo!")))
    .exec(ws("Close WS").close)

  setUp(
    scn.inject(
      rampUsers(1) over (1 seconds)
    ).protocols(httpConf)
  )
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


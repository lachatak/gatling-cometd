import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._

class GatlingTest extends Simulation {

  val httpConf = http
        .wsBaseURL("ws://echo.websocket.org")
//    .wsBaseURL("ws://localhost:8080")
    .disableFollowRedirect.disableWarmUp

  val scn = scenario("WebSocket")
    .exec(ws("Connect WS").open("/"))
    .pause(1 second)
    .exec(ws("echo").sendText("Echo!").check(wsAwait.within(5 seconds).until(1).message.is("Echo!")))
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


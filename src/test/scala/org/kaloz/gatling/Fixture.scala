package org.kaloz.gatling

import org.kaloz.gatling.http.cometd.CometDMessages.Published

object Fixture {

  val testJsonString = """{"message":"value","content":{"elem1":"value1","elem2":10}}"""

  case class TestObject(message: String = "value", content: Map[String, Any] = Map("elem1" -> "value1", "elem2" -> 10)) extends Published

}

package org.kaloz.gatling.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.reflect.runtime.{universe => ru}

object JsonUtil {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JodaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)

  def toJson(value: Any): String = {
    mapper.writeValueAsString(Array(value))
  }

  def fromJson[T: Manifest](json: String): T = {
    mapper.readValue[T](json)
  }
}

object JsonMarshallableImplicits {

  implicit class Unmarshallable(unMarshallMe: String) {
    def fromJson[T: Manifest]: T = JsonUtil.fromJson[T](unMarshallMe)
  }

  implicit class Marshallable[T](marshallMe: T) {
    def toJson: String = JsonUtil.toJson(marshallMe)
  }

}

package org.kaloz.gatling.json

import org.junit.runner.RunWith
import org.kaloz.gatling.Fixture._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class JsonUtilSpec extends Specification with AllExpectations {

  "JsonUtil" should {

    "generate correct object from string" in {

      val result = JsonUtil.fromJson[TestObject](testJsonString)

      result mustEqual TestObject()
    }

    "generate correct string from object" in {

      val result = JsonUtil.toJson(TestObject())

      result mustEqual testJsonString
    }

    "generate correct object from string using implicit" in {

      import org.kaloz.gatling.json.JsonMarshallableImplicits.Unmarshallable

      val result = testJsonString.fromJson[TestObject]

      result mustEqual TestObject()
    }

    "generate correct string from object using implicit" in {

      import org.kaloz.gatling.json.JsonMarshallableImplicits.Marshallable

      val result = TestObject().toJson

      result mustEqual testJsonString
    }
  }
}

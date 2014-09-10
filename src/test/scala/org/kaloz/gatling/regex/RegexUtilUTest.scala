package org.kaloz.gatling.regex

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class RegexUtilUTest extends Specification with AllExpectations {

  "RegexUtil" should {

    val text = "2cm 3px 4m 5km 6h"

    "generate correct regex string with 1 element" in {

      val result = RegexUtil.expression(Set("px")).r.findFirstIn(text)

      result mustEqual Some(text)
    }

    "generate correct forwardlooking regex string" in {

      val result = RegexUtil.expression(Set("px", "km")).r.findFirstIn(text)

      result mustEqual Some(text)
    }

    "generate correct forwardlooking regex strign in reversed order" in {

      val result = RegexUtil.expression(Set("km", "px")).r.findFirstIn(text)

      result mustEqual Some(text)
    }

    "generate correct forwardlooking regex string one element missing" in {

      val result = RegexUtil.expression(Set("px", "xxx")).r.findFirstIn(text)

      result mustEqual None
    }
  }
}

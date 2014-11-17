package org.kaloz.gatling.http.check

import java.util.concurrent.TimeUnit

import io.gatling.core.check.{CheckBase, CheckResult}
import io.gatling.core.session.Session
import io.gatling.core.validation
import io.gatling.http.check.ws.UntilCount
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.duration.Duration

class CometDCheckSpec extends WordSpecLike
with Matchers {

  "CometDCheck" should {

    val validMessage = """"id":"1", "successful":true"""
    val invalidMessage = "test"

    "generate correct wsCheck without saveAs and accept a valid response" in new scope {
      val cometDCheck = CometDCheck()(requestTimeOut)


      val wsCheck = cometDCheck.wsCheckBuilder.build

      wsCheck.blocking should be(true)
      wsCheck.timeout should be(requestTimeOut)
      wsCheck.expectation should be(UntilCount(1))
      wsCheck.wrapped.asInstanceOf[CheckBase[String, _, _]].saveAs should be(None)

      wsCheck.wrapped.check(validMessage, session)(cache).get should be(CheckResult(Some(validMessage), None))
    }

    "generate correct wsCheck with saveAs and accept a valid response meanwhile saving the result" in new scope {
      val cometDCheck = CometDCheck(saveAsName = Some("value"))(requestTimeOut)


      val wsCheck = cometDCheck.wsCheckBuilder.build

      wsCheck.blocking should be(true)
      wsCheck.timeout should be(requestTimeOut)
      wsCheck.expectation should be(UntilCount(1))
      wsCheck.wrapped.asInstanceOf[CheckBase[String, _, _]].saveAs should be(Some("value"))

      val checkResult = wsCheck.wrapped.check(validMessage, session)(cache).get
      checkResult should be(CheckResult(Some(validMessage), Some("value")))

      val newSession = checkResult.update.get(session)

      newSession.attributes.getOrElse("value", "NONE") should be(validMessage)
    }

    "generate correct wsCheck which failure for invalid response" in new scope {
      val cometDCheck = CometDCheck()(requestTimeOut)


      val wsCheck = cometDCheck.wsCheckBuilder.build

      wsCheck.blocking should be(true)
      wsCheck.timeout should be(requestTimeOut)
      wsCheck.expectation should be(UntilCount(1))
      wsCheck.wrapped.asInstanceOf[CheckBase[String, _, _]].saveAs should be(None)

      private val failureMessage: String = """regex((?=.*"id":"1")(?=.*"successful":true).*) transform.find(0).exists, found nothing"""
      wsCheck.wrapped.check(invalidMessage, session)(cache) should be(validation.Failure(failureMessage))
    }

    "generate correct wsCheck without saveAs and accept a valid response meanwhile applying the transformer" in new scope {
      val cometDCheck = CometDCheck(transformer = { m => m.reverse})(requestTimeOut)


      val wsCheck = cometDCheck.wsCheckBuilder.build

      wsCheck.blocking should be(true)
      wsCheck.timeout should be(requestTimeOut)
      wsCheck.expectation should be(UntilCount(1))
      wsCheck.wrapped.asInstanceOf[CheckBase[String, _, _]].saveAs should be(None)

      val checkResult = wsCheck.wrapped.check(validMessage, session)(cache).get
      checkResult should be(CheckResult(Some(validMessage.reverse), None))
    }
  }

  private trait scope {

    io.gatling.ConfigHook.setUpForTest()

    implicit val cache = mutable.Map.empty[Any, Any]
    implicit val requestTimeOut = Duration(5, TimeUnit.SECONDS)

    val session = Session("scenarioName", "userId", Map("cometDMessageId" -> 1))
  }

}

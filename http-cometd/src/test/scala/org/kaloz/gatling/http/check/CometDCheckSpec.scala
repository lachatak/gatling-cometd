package org.kaloz.gatling.http.check

import java.util.concurrent.TimeUnit

import akka.actor.Status.Success
import io.gatling.core.check.{ValidatorCheckBuilder, CheckBase}
import io.gatling.core.session.Session
import io.gatling.http.check.ws.UntilCount
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class CometDCheckSpec extends Specification with AllExpectations {

  "CometDCheck" should {

    "generate correct wsCheck" in {
      implicit val requestTimeOut = Duration(5, TimeUnit.SECONDS)

      val session = Session("scenarioName", "userId", Map("id" -> 1))

      val cometDCheck = CometDCheck()

      val wsCheck = cometDCheck.wsCheckBuilder.build

      wsCheck.blocking mustEqual true
      wsCheck.timeout mustEqual requestTimeOut
      wsCheck.expectation mustEqual UntilCount(1)
      wsCheck.wrapped.asInstanceOf[CheckBase[_, _, _]].saveAs mustEqual None
//      wsCheck.wrapped.asInstanceOf[CheckBase[_, _, _]].preparer mustEqual None
//      wsCheck.wrapped.asInstanceOf[CheckBase[_, _, _]].extractorExpression(session).toString mustEqual ""
//      wsCheck.wrapped.asInstanceOf[CheckBase[_, _, _]].validatorExpression mustEqual None
    }
  }

}

package org.kaloz.gatling.http.cometd

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import io.gatling.core.Predef._
import io.gatling.core.action.builder.{ActionBuilder, LoopBuilder}
import io.gatling.core.session._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import org.kaloz.gatling.http.request.builder.cometd.CometD

import scala.concurrent.duration.{Duration, FiniteDuration}

object Predef {

  def cometD(requestName: Expression[String])(implicit requestTimeOut: FiniteDuration) = new CometD(requestName)

  def execCometD(actionBuilder: ActionBuilder): ChainBuilder = exec(feed(idFeeder).exec(actionBuilder))

  val idGenerator = new AtomicLong(1)
  val idFeeder = Iterator.continually(Map("id" -> idGenerator.getAndIncrement))

  implicit class CometDScenarioBuilder(scenarioBuilder: ScenarioBuilder)(implicit requestTimeOut: FiniteDuration) {
    def execCometD(actionBuilder: ActionBuilder): ScenarioBuilder = scenarioBuilder.feed(idFeeder).exec(actionBuilder)
  }

  implicit class CometDChainBuilder(chainBuilder: ChainBuilder)(implicit requestTimeOut: FiniteDuration) {
    def execCometD(actionBuilder: ActionBuilder): ChainBuilder = chainBuilder.feed(idFeeder).exec(actionBuilder)

    def waitFor(condition: Expression[Boolean], min: Duration = 1, max: Duration = 2): ChainBuilder = {
      chainBuilder.exec(new LoopBuilder(condition, pause(min, max).exec(cometD("reconciliate").reconciliate), UUID.randomUUID.toString, false))
    }
  }

}

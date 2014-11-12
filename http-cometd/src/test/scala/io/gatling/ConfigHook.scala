package io.gatling

import io.gatling.core.config.GatlingConfiguration

object ConfigHook {

  def setUpForTest() = GatlingConfiguration.setUpForTest()
}

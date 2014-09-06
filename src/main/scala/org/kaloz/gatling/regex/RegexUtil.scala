package org.kaloz.gatling.regex

object RegexUtil {

  def expression(matchers: Set[String]): String = matchers.mkString("(?=.*", ")(?=.*", ").*")
}

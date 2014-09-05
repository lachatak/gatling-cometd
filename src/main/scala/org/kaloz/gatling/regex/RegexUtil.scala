package org.kaloz.gatling.regex

object RegexUtil {

  def expression(matchers: List[String]): String = matchers.mkString("(?=.*", ")(?=.*", ").*")
}

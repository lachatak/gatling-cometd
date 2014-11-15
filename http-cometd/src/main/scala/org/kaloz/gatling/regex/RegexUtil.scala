package org.kaloz.gatling.regex

object RegexUtil {

  def containsAll(matchers: Set[String]): String = matchers.mkString("(?=.*", ")(?=.*", ").*")
}

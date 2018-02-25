package matchers

import org.scalactic.Prettifier
import org.scalatest.matchers.{MatchResult, Matcher}
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode, JSONCompareResult}

/**
  * Created by alex on 23/02/18
  **/

trait JsonMatchers {

  def equalsToJson(spread: String): Matcher[String] = {
    new Matcher[String] {
      def apply(left: String): MatchResult = {
        val result: JSONCompareResult = JSONCompare.compareJSON(left, spread, JSONCompareMode.STRICT_ORDER)
        MatchResult(
          !result.failed(),
          "json are not equal",
          result.getMessage
        )
      }
      override def toString: String = "jsonEqual (" + Prettifier.default(spread) + ")"
    }
  }

}
package squeezebox

import org.scalatest._

/**
  * Created by alex on 24/12/17
  **/
class RomanNumeralsServiceImplSpec extends FlatSpec with Matchers {

  val romanNumerals = new RomanNumeralsServiceImpl

  behavior of "roman numerals"

  it should "not replace roman numerals that are part of a word" in {
    val result = romanNumerals.replace("Hawaii Five O")
    result should equal("Hawaii Five O")
  }

  it should "do nothing to strings that have no roman numerals" in {
    val result = romanNumerals.replace("A Night at the Opera")
    result should equal("A Night at the Opera")
  }

  it should "replace numerals at the end of a string" in {
    val result = romanNumerals.replace("Queen II")
    result should equal("Queen 2")
  }

  it should "replace numerals at the beginning of a string" in {
    val result = romanNumerals.replace("IX: Something")
    result should equal("9: Something")
  }

  it should "replace numerals in the middle of a string" in {
    val result = romanNumerals.replace("Abigail II: The Return")
    result should equal("Abigail 2: The Return")
  }
}

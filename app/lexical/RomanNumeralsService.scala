package lexical

/**
  * Created by alex on 24/12/17
  *
  * Convert roman numerals to numbers and vice versa. This is to allow albums such as Queen II to be matched by someone
  * saying "Queen 2".
  **/
trait RomanNumeralsService {

  /**
    * Convert a string to roman numerals.
    * @param str The string to convert.
    * @return The value of the roman numeral or the first found invalid character and its position.
    */
  def toInt(str: String): Either[(Int, Char), Int]

  /**
    * Replace all instances of roman numerals with ther numeric value.
    * @param str The string to search for roman numerals.
    * @return The string with any roman numerals replaced with numbers.
    */
  def replace(str: String): String
}

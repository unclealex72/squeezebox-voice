package lexical

/**
  * Created by alex on 24/12/17
  *
  * The default implementation of [[RomanNumeralsService]].
  **/
class RomanNumeralsServiceImpl extends RomanNumeralsService {
  /**
    * Convert a string to roman numerals.
    *
    * @param str The string to convert.
    * @return The value of the roman numeral or a list of invalid characters by their position in the string.
    */
  override def toInt(str: String): Either[(Int, Char), Int] = {
    def toInt_(chars: List[Char], idx: Int): Either[(Int, Char), Int] = {
      chars match {
        case 'i' :: 'v' :: xs => toInt_(xs, idx + 2).map(_ + 4)
        case 'i' :: 'x' :: xs => toInt_(xs, idx + 2).map(_ + 9)
        case 'i' :: xs        => toInt_(xs, idx + 1).map(_ + 1)
        case 'v' :: xs        => toInt_(xs, idx + 1).map(_ + 5)
        case 'x' :: 'l' :: xs => toInt_(xs, idx + 2).map(_ + 40)
        case 'x' :: 'c' :: xs => toInt_(xs, idx + 2).map(_ + 90)
        case 'x' :: xs        => toInt_(xs, idx + 1).map(_ + 10)
        case 'l' :: xs        => toInt_(xs, idx + 1).map(_ + 50)
        case 'c' :: 'd' :: xs => toInt_(xs, idx + 2).map(_ + 400)
        case 'c' :: 'm' :: xs => toInt_(xs, idx + 2).map(_ + 900)
        case 'c' :: xs        => toInt_(xs, idx + 1).map(_ + 100)
        case 'd' :: xs        => toInt_(xs, idx + 1).map(_ + 500)
        case 'm' :: xs        => toInt_(xs, idx + 1).map(_ + 1000)
        case x :: _           => Left(idx -> x)
        case Nil              => Right(0)
      }
    }
    val chars = str.toLowerCase.toList
    toInt_(chars, 0)
  }

  /**
    * Replace all instances of roman numerals with ther numeric value.
    *
    * @param str The string to search for roman numerals.
    * @return The string with any roman numerals replaced with numbers.
    */
  override def replace(str: String): String = {
    val romanNumeralRegex = """\b[ivxlcdmIVXLCDM]+\b""".r
    romanNumeralRegex.replaceSomeIn(str, m => toInt(m.toString).toOption.map(_.toString))
  }
}

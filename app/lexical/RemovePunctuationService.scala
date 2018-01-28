package lexical

/**
  * Created by alex on 27/01/18
  *
  * Remove punctuation and non-ASCII characters from strings.
  **/
trait RemovePunctuationService {

  /**
    * Remove punctuation and non-ASCII characters a string.
    * @param str A string that may contain punctuation.
    * @return A string with only alphanumeric ASCII characters and spaces.
    */
  def apply(str: String): String
}

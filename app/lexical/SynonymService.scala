package lexical

/**
  * Created by alex on 24/12/17
  *
  * Create synonyms for entries so that one entry can be matched by more than one phrase.
  **/
trait SynonymService {

  /**
    * Generate a list of synonyms.
    * @param str The string to use as a base.
    * @return A list of synonms for the original string.
    */
  def apply(str: String): Seq[String]
}

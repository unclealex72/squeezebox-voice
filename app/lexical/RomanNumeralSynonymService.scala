package lexical

/**
  * Created by alex on 24/12/17
  *
  * The default implementation of [[SynonymService]] that creates synonyms by converting roman numerals to digits.
  **/
class RomanNumeralSynonymService(romanNumeralsService: RomanNumeralsService) extends SynonymService {
  override def apply(str: String): Seq[String] = {
    Some(romanNumeralsService.replace(str)).filter(result => result != str).toSeq
  }
}

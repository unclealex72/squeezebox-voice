package squeezebox

import javax.inject.Inject

/**
  * Created by alex on 24/12/17
  **/
class RomanNumeralSynonymService @Inject() (romanNumeralsService: RomanNumeralsService) extends SynonymService {
  override def synonyms(str: String): Seq[String] = {
    Some(romanNumeralsService.replace(str)).filter(result => result != str).toSeq
  }
}

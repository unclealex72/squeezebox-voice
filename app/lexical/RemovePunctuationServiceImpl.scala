package lexical

import java.text.Normalizer

/**
  * Created by alex on 27/01/18
  *
  * The default implementation of [[RemovePunctuationService]].
  **/
class RemovePunctuationServiceImpl extends RemovePunctuationService {

  def apply(str: String): String = {
    val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
    val normalizedWithoutTurkish = normalized.replace('\u0131', 'i').replace('\u0130', 'I')
    normalizedWithoutTurkish.filter {
      ch => Character.isLetterOrDigit(ch) || ch == ' '
    }
  }

}

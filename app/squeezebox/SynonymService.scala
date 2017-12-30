package squeezebox

/**
  * Created by alex on 24/12/17
  **/
trait SynonymService {

  def synonyms(str: String): Seq[String]
}

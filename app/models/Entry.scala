package models

/**
  * Created by alex on 25/01/18
  *
  * An entry encapsulates an entry in a DialogFlow entity.
  **/
case class Entry(
                  /**
                    * The entry without punctuation.
                    */
                  unpunctuated: String,
                  /**
                    * Synonyms for the entry.
                    */
                  synonyms: Seq[String] = Seq.empty)

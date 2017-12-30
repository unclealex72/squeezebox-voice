package squeezebox

import scala.collection.SortedSet

/**
  * Created by alex on 23/12/17
  **/
case class Album(title: String, unpunctuatedTitle: String, artists: SortedSet[String], synonyms: Seq[String])
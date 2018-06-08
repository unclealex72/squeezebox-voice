package webhook

/**
  * Created by alex on 11/02/18
  **/
import enumeratum._

import scala.collection.immutable.IndexedSeq

sealed case class Context(name: String) extends EnumEntry

object Context extends Enum[Context] with JsonCodec[Context] {
  override def tokenFactory: Context => String = _.name

  override val name: String = "context"
  override val values: IndexedSeq[Context] = findValues

  object ArtistRequired extends Context("artist-required-context")

}
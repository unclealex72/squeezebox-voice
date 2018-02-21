package webhook

/**
  * Created by alex on 11/02/18
  **/
import enumeratum._
import scala.collection.immutable.IndexedSeq

sealed case class Parameter(name: String) extends EnumEntry

object Parameter extends Enum[Parameter] with JsonCodec[Parameter] {

  override val values: IndexedSeq[Parameter] = findValues
  override def tokenFactory: Parameter => String = _.name
  override val name: String = "parameter"

  object Room extends Parameter("room")
  object Album extends Parameter("album")
  object Artist extends Parameter("artist")
  object Favourite extends Parameter("favourite")
  object Playlist extends Parameter("playlist")
  object CurrentTitle extends Parameter("currentTitle")
  object CurrentArtist extends Parameter("currentArtist")
  object Albums extends Parameter("albums")
  object Artists extends Parameter("artists")

}
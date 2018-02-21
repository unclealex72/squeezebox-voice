package webhook

/**
  * Created by alex on 11/02/18
  **/
import enumeratum._
import scala.collection.immutable.IndexedSeq

sealed case class Event(event: String) extends EnumEntry

object Event extends Enum[Event] with JsonCodec[Event] {

  override val values: IndexedSeq[Event] = findValues
  override def tokenFactory: Event => String = _.event
  override val name: String = "event"

  object RoomNotConnected extends Event("room-not-connected")
  object PlayingFavourite extends Event("playing-favourite")
  object PlayingPlaylist extends Event("playing-playlist")
  object CurrentlyPlaying extends Event("currently-playing")
  object NothingPlaying extends Event("nothing-playing")
  object WrongArtist extends Event("wrong-artist")
  object AlbumsForArtist extends Event("albums-for-artist")
  object PlayingAlbum extends Event("playing-album")
  object ArtistRequired extends Event("artist-required")
  object Updating extends Event("updating")

}
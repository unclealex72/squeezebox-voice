package webhook

/**
  * Created by alex on 11/02/18
  **/
import enumeratum._

import scala.collection.immutable.IndexedSeq

sealed case class Action(action: String) extends EnumEntry

object Action extends Enum[Action] with JsonCodec[Action] {

  override def tokenFactory: Action => String = _.action
  override val name: String = "action"
  override val values: IndexedSeq[Action] = findValues

  object PlayFavourite extends Action("play-favourite")
  object PlayPlaylist extends Action("play-playlist")
  object PlayAlbum extends Action("play-album")
  object ProvideRequiredArtist extends Action("provide-required-artist")
  object NowPlaying extends Action("now-playing")
  object BrowseArtist extends Action("browse-artist")
  object Update extends Action("update")

}
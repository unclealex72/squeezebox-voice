package webhook

import cats.data.ValidatedNel
import cats.implicits._
import enumeratum.{Enum, EnumEntry}
import media.MediaCacheView
import models._

import scala.collection.immutable.IndexedSeq

/**
  * Created by alex on 28/01/18
  *
  * A wrapper around a map of strings that contain parameters used by dialogFow.
  **/
case class WebhookParameters(
                              maybeRoomName: Option[String] = None,
                              maybeAlbumTitle: Option[String] = None,
                              maybeArtistName: Option[String] = None,
                              maybeFavouriteName: Option[String] = None,
                              maybePlaylistName: Option[String] = None,
                              maybeCurrentTitle: Option[String] = None,
                              maybeCurrentArtist: Option[String] = None,
                              maybeAlbums: Option[Seq[String]] = None,
                              maybeArtists: Option[Seq[String]] = None) {

  import webhook.WebhookParameters.Parameter

  /**
    * Set the room stored in these parameters.
    * @param room The room to store.
    * @return A copy of these parameters with the room added.
    */
  def withRoom(room: Room): WebhookParameters = copy(maybeRoomName = Some(room.name))

  /**
    * Set the album stored in these parameters.
    * @param album The album to store.
    * @return A copy of these parameters with the album added.
    */
  def withAlbum(album: Album): WebhookParameters = copy(maybeAlbumTitle = Some(album.title))

  /**
    * Set the artist stored in these parameters.
    * @param artist The artist to store.
    * @return A copy of these parameters with the artist added.
    */
  def withArtist(artist: Artist): WebhookParameters = copy(maybeArtistName = Some(artist.name))
  
  /**
    * Set the favourite stored in these parameters.
    * @param favourite The favourite to store.
    * @return A copy of these parameters with the favourite added.
    */
  def withFavourite(favourite: Favourite): WebhookParameters = copy(maybeFavouriteName = Some(favourite.name))
  
  /**
    * Set the playlist stored in these parameters.
    * @param playlist The playlist to store.
    * @return A copy of these parameters with the playlist added.
    */
  def withPlaylist(playlist: Playlist): WebhookParameters = copy(maybePlaylistName = Some(playlist.name))

  /**
    * Set the the title of the song currently playing stored in these parameters.
    * @param currentTitle The title of the song currently playing to store.
    * @return A copy of these parameters with the the title of the song currently playing added.
    */
  def withCurrentTitle(currentTitle: String): WebhookParameters = copy(maybeCurrentTitle = Some(currentTitle))

  /**
    * Set the the artist of the song currently playing stored in these parameters.
    * @param currentArtist The the artist of the song currently playing to store.
    * @return A copy of these parameters with the the artist of the song currently playing added.
    */
  def withCurrentArtist(currentArtist: String): WebhookParameters = copy(maybeCurrentArtist = Some(currentArtist))
  
  /**
    * Set the albums stored in these parameters.
    * @param albums The albums to store.
    * @return A copy of these parameters with the albums added.
    */
  def withAlbums(albums: Seq[Album]): WebhookParameters = copy(maybeAlbums = Some(albums.map(_.title)))

  /**
    * Set the albums stored in these parameters.
    * @param album The first albums to store.
    * @param albums The subsequent albums to store.
    * @return A copy of these parameters with the albums added.
    */
  def withAlbums(album: Album, albums: Album*): WebhookParameters = withAlbums(album +: albums)
  
  /**
    * Set the artists stored in these parameters.
    * @param artists The album to store.
    * @return A copy of these parameters with the artists added.
    */
  def withArtists(artists: Seq[Artist]): WebhookParameters = copy(maybeArtists = Some(artists.map(_.name)))

  /**
    * Set the artists stored in these parameters.
    * @param artist The first artists to store.
    * @param artists The subsequent artists to store.
    * @return A copy of these parameters with the artists added.
    */
  def withArtists(artist: Artist, artists: Artist*): WebhookParameters = withArtists(artist +: artists)

  private def optional[A](parameter: Parameter, maybeValue: Option[String], finder: String => Option[A]): ValidatedNel[String, Option[A]] = {
    maybeValue.filterNot(_.isEmpty) match {
      case Some(value) => finder(value).toRight(s"$value is not a valid ${parameter.name}").toValidatedNel.map(Some(_))
      case None => None.validNel
    }
  }

  private def mandatory[A](parameter: Parameter, maybeValue: Option[String], finder: String => Option[A]): ValidatedNel[String, A] = {
    optional(parameter, maybeValue, finder) andThen {
      case Some(value) => value.validNel
      case None => s"${parameter.name} is required".invalidNel
    }
  }

  /**
    * Get the room stored in these parameters.
    * @return The room or invalid if it cannot be found.
    */
  def room(mediaCache: MediaCacheView): ValidatedNel[String, Room] =
    mandatory(Parameter.Room, maybeRoomName, mediaCache.player)

  /**
    * Get the favourite stored in these parameters.
    * @return The favourite or invalid if it cannot be found.
    */
  def favourite(mediaCache: MediaCacheView): ValidatedNel[String, Favourite] =
    mandatory(Parameter.Favourite, maybeFavouriteName, mediaCache.favourite)

  /**
    * Get the playlist stored in these parameters.
    * @return The playlist or invalid if it cannot be found.
    */
  def playlist(mediaCache: MediaCacheView): ValidatedNel[String, Playlist] =
    mandatory(Parameter.Playlist, maybePlaylistName, mediaCache.playlist)

  /**
    * Get the album stored in these parameters.
    * @return The album or invalid if it cannot be found.
    */
  def album(mediaCache: MediaCacheView): ValidatedNel[String, Album] =
    mandatory(Parameter.Album, maybeAlbumTitle, mediaCache.album)

  /**
    * Get the artist stored in these parameters.
    * @return The artist or invalid if it cannot be found.
    */
  def artist(mediaCache: MediaCacheView): ValidatedNel[String, Artist] =
    mandatory(Parameter.Artist, maybeArtistName, mediaCache.artist)

  /**
    * Get the artist stored in these parameters.
    * @return If the parameter exists return the artist or invalid if it cannot be found. Otherwise return None.
    */
  def maybeArtist(mediaCache: MediaCacheView): ValidatedNel[String, Option[Artist]] =
    optional(Parameter.Artist, maybeArtistName, mediaCache.artist)

}

object WebhookParameters {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  /**
    * An enumeration for the parameters stored in [[WebhookParameters]]
    * @param name The name of the parameter used when serialising to and deserialising from JSON.
    */
  private[WebhookParameters] sealed case class Parameter(name: String) extends EnumEntry

  /**
    * The valid parameters.
    */
  private[WebhookParameters] object Parameter extends Enum[Parameter] {

    override val values: IndexedSeq[Parameter] = findValues

    /**
      * The parameter for the current room.
      */
    object Room extends Parameter("room")

    /**
      * The album parameter used for album searches.
      */
    object Album extends Parameter("album")

    /**
      * The artist parameter used for album searches.
      */
    object Artist extends Parameter("artist")

    /**
      * The favourite parameter used for favourite searches.
      */
    object Favourite extends Parameter("favourite")

    /**
      * The playlist parameter used for playlist searches.
      */
    object Playlist extends Parameter("playlist")

    /**
      * The current title parameter used to show the title of the currently playing song.
      */
    object CurrentTitle extends Parameter("currentTitle")

    /**
      * The current artist parameter used to show the artists of the currently playing song.
      */
    object CurrentArtist extends Parameter("currentArtist")

    /**
      * The parameter used to list the albums for an artists.
      */
    object Albums extends Parameter("albums")

    /**
      * The parameter used to list the artists for an album.
      */
    object Artists extends Parameter("artists")

  }

  /**
    * Allow [[Parameter]]s to be treated like strings.
    * @param jsPath The jsPath to extend.
    */
  implicit class JsPathExtensions(jsPath: JsPath) {
    def \ (parameter: Parameter): JsPath = jsPath \ parameter.name
  }

  /**
    * Read parameters from JSON.
    *
    * @return A reader for reading parameters from JSON.
    */
  implicit val webhookParametersReads: Reads[WebhookParameters] = (
    (JsPath \ Parameter.Room).readNullable[String] and
      (JsPath \ Parameter.Album).readNullable[String] and
      (JsPath \ Parameter.Artist).readNullable[String] and
      (JsPath \ Parameter.Favourite).readNullable[String] and
      (JsPath \ Parameter.Playlist).readNullable[String] and
      (JsPath \ Parameter.CurrentTitle).readNullable[String] and
      (JsPath \ Parameter.CurrentArtist).readNullable[String] and
      (JsPath \ Parameter.Albums).readNullable[Seq[String]] and
      (JsPath \ Parameter.Artists).readNullable[Seq[String]]
    ) (WebhookParameters.apply _)

  /**
    * Write parameters to JSON.
    *
    * @return A writer for writing parameters to JSON.
    */
  implicit val webhookParametersWrites: Writes[WebhookParameters] = (
    (JsPath \ Parameter.Room).writeNullable[String] and
      (JsPath \ Parameter.Album).writeNullable[String] and
      (JsPath \ Parameter.Artist).writeNullable[String] and
      (JsPath \ Parameter.Favourite).writeNullable[String] and
      (JsPath \ Parameter.Playlist).writeNullable[String] and
      (JsPath \ Parameter.CurrentTitle).writeNullable[String] and
      (JsPath \ Parameter.CurrentArtist).writeNullable[String] and
      (JsPath \ Parameter.Albums).writeNullable[Seq[String]] and
      (JsPath \ Parameter.Artists).writeNullable[Seq[String]]
    ) (unlift(WebhookParameters.unapply))
}
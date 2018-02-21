package squeezebox

import models.{Album, Favourite, Playlist, Room}

import scala.concurrent.Future

/**
  * An interface to get rooms, albums, favourites and playlists from a music repository.
  **/
trait MusicRepository {

  /**
    * Get a list of all known albums.
    * @return A list of all known albums.
    */
  def albums: Future[Set[Album]]

  /**
    * Get a list of all known favourites.
    * @return A list of all known favourites.
    */
  def favourites: Future[Set[Favourite]]

  /**
    * Get a list of all known playlists.
    * @return A list of all known playlists.
    */
  def playlists: Future[Set[Playlist]]

  /**
    * Get a list of all connected players.
    * @return A list of all known players.
    */
  def rooms: Future[Set[Room]]


}

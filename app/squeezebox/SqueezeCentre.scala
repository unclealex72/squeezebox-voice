package squeezebox

import models.{Album, Artist, Favourite, Room}

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}

/**
  * Interface with a squeezebox to get players, albums, artists and to play albums.
  * Created by alex on 23/12/17
  **/
trait SqueezeCentre {

  /**
    * Get a list of all known albums.
    * @return A list of all known albums.
    */
  def albums: Future[Seq[Album]]

  /**
    * Get a list of all known favourites.
    * @return A list of all known favourites.
    */
  def favourites: Future[SortedSet[Favourite]]

  /**
    * Get a list of all connected players.
    * @return A list of all known players.
    */
  def rooms: Future[SortedSet[Room]]

  /**
    * Play an album on a player.
    * @param player The player that will play the album.
    * @param album The album to play.
    * @param artist The artist of the album
    * @return Unit.
    */
  def playAlbum(player: Room, album: Album, artist: Artist): Future[Unit]

  /**
    * Play a favourite on a player.
    * @param player The player that will play the album.
    * @param favourite The favourite to play.
    * @return Unit.
    */
  def playFavourite(player: Room, favourite: Favourite): Future[Unit]
}

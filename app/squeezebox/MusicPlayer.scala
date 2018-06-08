package squeezebox

import models._

import scala.concurrent.Future

/**
  * An Interface to play albums, favourites and playlists and also to check which players are currently connected.
  * Created by alex on 23/12/17
  **/
trait MusicPlayer {

  /**
    * Get the current [[PlaylistInfo]] from a player.
    * @param room The room the player is in.
    * @return Eventually the [[PlaylistInfo]] if it can be found or none if it cannot.
    */
  def playlistInformation(room: Room): Future[Option[PlaylistInfo]]

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
    * @param player The player that will play the favourite.
    * @param favourite The favourite to play.
    * @return Unit.
    */
  def playFavourite(player: Room, favourite: Favourite): Future[Unit]

  /**
    * Play a playlist on a player.
    * @param player The player that will play the playlist.
    * @param playlist The playlist to play.
    * @return Unit.
    */
  def playPlaylist(player: Room, playlist: Playlist): Future[Unit]

  /**
    * Get a list of all connected players.
    * @return A list of all known players.
    */
  def connectedRooms(): Future[Set[Room]]


}


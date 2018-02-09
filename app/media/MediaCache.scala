package media

import models._

/**
  * A cache for all media stored on the media centre.
  * Created by alex on 26/12/17
  **/
trait MediaCache {

  /**
    * Find the album with the given title.
    *
    * @param title The title to look for (case insensitive)
    * @return The album with the given title or none.
    */
  def album(title: String): Option[Album]

  /**
    * Find the artist with the given name.
    * @param name The name to look for (case insensitive)
    * @return The artist with the given name or none.
    */
  def artist(name: String): Option[Artist]

  /**
    * Find the player with the given name.
    * @param name The name to look for (case insensitive)
    * @return The player with the given name or none.
    */
  def player(name: String): Option[Room]

  /**
    * List all albums for an artist.
    * @param artist The artist to search for.
    * @return A list of all albums recorded by the artist.
    */
  def listAlbums(artist: Artist): Seq[Album]

  /**
    * Find the favourite with the given name.
    * @param name The name to look for (case insensitive)
    * @return The favourite with the given name or none.
    */
  def favourite(name: String): Option[Favourite]

  /**
    * Find the playlist with the given name.
    * @param name The name to look for (case insensitive)
    * @return The playlist with the given name or none.
    */
  def playlist(name: String): Option[Playlist]

  /**
    * Update the albums.
    * @param albums All known albums.
    */
  def updateAlbums(albums: Seq[Album]): Seq[Album]

  /**
    * Update the artists.
    * @param artists All known artists.
    */
  def updateArtists(artists: Seq[Artist]): Seq[Artist]

  /**
    * Update the rooms.
    * Note that rooms that are no longer connected are not removed.
    * @param rooms All known rooms.
    */
  def updateRooms(rooms: Seq[Room]): Seq[Room]

  /**
    * Update the favourites.
    * @param favourites All known favourites.
    */
  def updateFavourites(favourites: Seq[Favourite]): Seq[Favourite]

  /**
    * Update the playlists.
    * @param playlists All known playlists.
    */
  def updatePlaylists(playlists: Seq[Playlist]): Seq[Playlist]
}

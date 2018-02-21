package media

import models._

/**
  * A read-only cache for all media stored on the media centre.
  * Created by alex on 26/12/17
  **/
trait MediaCacheView {

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
  def listAlbums(artist: Artist): Set[Album]

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

}

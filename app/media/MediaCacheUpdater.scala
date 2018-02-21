package media

import models._

/**
  * An interface for updating the cache that's viewable from [[MediaCacheView]]
  **/
trait MediaCacheUpdater {

  /**
    * Update the albums.
    * @param albums All known albums.
    */
  def updateAlbums(albums: Set[Album]): Set[Album]

  /**
    * Update the artists.
    * @param artists All known artists.
    */
  def updateArtists(artists: Set[Artist]): Set[Artist]

  /**
    * Update the rooms.
    * Note that rooms that are no longer connected are not removed.
    * @param rooms All known rooms.
    */
  def updateRooms(rooms: Set[Room]): Set[Room]

  /**
    * Update the favourites.
    * @param favourites All known favourites.
    */
  def updateFavourites(favourites: Set[Favourite]): Set[Favourite]

  /**
    * Update the playlists.
    * @param playlists All known playlists.
    */
  def updatePlaylists(playlists: Set[Playlist]): Set[Playlist]
}

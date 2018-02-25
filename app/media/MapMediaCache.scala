package media

import javax.inject.{Inject, Singleton}

import lexical.RemovePunctuationService
import models._

/**
  * A [[MediaCacheView]] that uses volatile hash maps.
  * Created by alex on 26/12/17
  **/
@Singleton
class MapMediaCache @Inject() (removePunctuation: RemovePunctuationService) extends MediaCacheView with MediaCacheUpdater {

  @volatile var albumMap: Map[String, Album] = Map.empty
  @volatile var artistMap: Map[String, Artist] = Map.empty
  @volatile var roomMap: Map[String, Room] = Map.empty
  @volatile var favouriteMap: Map[String, Favourite] = Map.empty
  @volatile var playlistMap: Map[String, Playlist] = Map.empty

  /**
    * Find the album with the given title.
    *
    * @param title The title to look for (case insensitive)
    * @return The album with the given title or none.
    */
  override def album(title: String): Option[Album] = {
    albumMap.get(removePunctuation(title).toLowerCase)
  }

  /**
    * Find the artist with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The artist with the given title or none.
    */
  override def artist(name: String): Option[Artist] = {
    artistMap.get(removePunctuation(name).toLowerCase)
  }

  /**
    * Find the player with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The player with the given name or none.
    */
  override def player(name: String): Option[Room] = {
    roomMap.get(removePunctuation(name).toLowerCase)
  }

  /**
    * Find the favourite with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The favourite with the given name or none.
    */
  override def favourite(name: String): Option[Favourite] = {
    favouriteMap.get(removePunctuation(name).toLowerCase)
  }

  /**
    * Find the playlist with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The playlist with the given name or none.
    */
  override def playlist(name: String): Option[Playlist] = {
    playlistMap.get(removePunctuation(name).toLowerCase)
  }

  /**
    * List all albums for an artist.
    *
    * @param artist The artist to search for.
    * @return A list of all albums recorded by the artist.
    */
  override def listAlbums(artist: Artist): Set[Album] = {
    albumMap.values.toSet.filter(album => album.artists.contains(artist))
  }

  implicit class UniqueGroupingImplicits[V](values: Set[V]) {

    def uniquelyGroupBy(keyBuilder: V => String): Map[String, V] = {
      val empty: Map[String, V] = Map.empty
      values.foldLeft(empty) { (map, value) =>
        map + (keyBuilder(value) -> value)
      }
    }
  }

  /**
    * Update the albums.
    *
    * @param albums All known albums.
    */
  override def updateAlbums(albums: Set[Album]): Set[Album] = {
    albumMap = albums.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    albums
  }

  /**
    * Update the artists.
    *
    * @param artists All known artists.
    */
  override def updateArtists(artists: Set[Artist]): Set[Artist] = {
    artistMap = artists.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    artists
  }

  /**
    * Update the players.
    *
    * @param rooms All known players.
    */
  override def updateRooms(rooms: Set[Room]): Set[Room] = {
    roomMap = rooms.uniquelyGroupBy(_.name.toLowerCase)
    roomMap.values.toSet
  }

  /**
    * Update the favourites.
    *
    * @param favourites All known favourites.
    */
  override def updateFavourites(favourites: Set[Favourite]): Set[Favourite] = {
    favouriteMap = favourites.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    favourites
  }

  /**
    * Update the playlists.
    *
    * @param playlists All known playlists.
    */
  override def updatePlaylists(playlists: Set[Playlist]): Set[Playlist] = {
    playlistMap = playlists.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    playlists
  }
}

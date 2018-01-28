package media

import javax.inject.Inject

import lexical.RemovePunctuationService
import models.{Album, Artist, Favourite, Room}

/**
  * A [[MediaCache]] that uses volatile hash maps.
  * Created by alex on 26/12/17
  **/
class MapMediaCache @Inject() (removePunctuation: RemovePunctuationService) extends MediaCache {

  @volatile var albumMap: Map[String, Album] = Map.empty
  @volatile var artistMap: Map[String, Artist] = Map.empty
  @volatile var roomMap: Map[String, Room] = Map.empty
  @volatile var favouriteMap: Map[String, Favourite] = Map.empty

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

  implicit class UniqueGroupingImplicits[V](values: Seq[V]) {

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
  override def updateAlbums(albums: Seq[Album]): Seq[Album] = {
    albumMap = albums.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    albums
  }

  /**
    * Update the artists.
    *
    * @param artists All known artists.
    */
  override def updateArtists(artists: Seq[Artist]): Seq[Artist] = {
    artistMap = artists.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    artists
  }

  /**
    * Update the players.
    *
    * @param rooms All known players.
    */
  override def updateRooms(rooms: Seq[Room]): Seq[Room] = {
    roomMap = roomMap ++ rooms.uniquelyGroupBy(_.name.toLowerCase)
    (rooms ++ roomMap.values).distinct
  }

  /**
    * Update the favourites.
    *
    * @param favourites All known favourites.
    */
  override def updateFavourites(favourites: Seq[Favourite]): Seq[Favourite] = {
    favouriteMap = favourites.uniquelyGroupBy(_.entry.unpunctuated.toLowerCase)
    favourites
  }
}

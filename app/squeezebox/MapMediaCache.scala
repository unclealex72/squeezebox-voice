package squeezebox

/**
  * A `MediaCache` that uses volatile hash maps.
  * Created by alex on 26/12/17
  **/
class MapMediaCache extends MediaCache {

  @volatile var albumMap: Map[String, Album] = Map.empty
  @volatile var playerMap: Map[String, Player] = Map.empty
  @volatile var favouriteMap: Map[String, Favourite] = Map.empty

  /**
    * Find the album with the given title.
    *
    * @param unpunctuatedTitle The title (without punctuation) to look for (case insensitive)
    * @return The album with the given title or none.
    */
  override def album(unpunctuatedTitle: String): Option[Album] = {
    albumMap.get(unpunctuatedTitle.toLowerCase)
  }

  /**
    * Find the player with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The player with the given name or none.
    */
  override def player(name: String): Option[Player] = {
    playerMap.get(name.toLowerCase)
  }

  /**
    * Find the favourite with the given name.
    *
    * @param name The name to look for (case insensitive)
    * @return The favourite with the given name or none.
    */
  override def favourite(name: String): Option[Favourite] = {
    favouriteMap.get(name.toLowerCase)
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
  override def updateAlbums(albums: Seq[Album]): Unit = {
    albumMap = albums.uniquelyGroupBy(_.unpunctuatedTitle.toLowerCase)
  }

  /**
    * Update the players.
    *
    * @param players All known players.
    */
  override def updatePlayers(players: Seq[Player]): Unit = {
    playerMap = players.uniquelyGroupBy(_.name.toLowerCase)
  }

  /**
    * Update the favourites.
    *
    * @param favourites All known favourites.
    */
  override def updateFavourites(favourites: Seq[Favourite]): Unit = {
    favouriteMap = favourites.uniquelyGroupBy(_.name.toLowerCase)
  }
}

package squeezebox

/**
  * A cache for all media stored on the media centre.
  * Created by alex on 26/12/17
  **/
trait MediaCache {

  /**
    * Find the album with the given title.
    * @param title The title to look for (case insensitive)
    * @return The album with the given title or none.
    */
  def album(title: String): Option[Album]

  /**
    * Find the player with the given name.
    * @param name The name to look for (case insensitive)
    * @return The player with the given name or none.
    */
  def player(name: String): Option[Player]

  /**
    * Find the favourite with the given name.
    * @param name The name to look for (case insensitive)
    * @return The favourite with the given name or none.
    */
  def favourite(name: String): Option[Favourite]

  /**
    * Update the albums.
    * @param albums All known albums.
    */
  def updateAlbums(albums: Seq[Album]): Unit

  /**
    * Update the players.
    * @param players All known players.
    */
  def updatePlayers(players: Seq[Player]): Unit

  /**
    * Update the favourites.
    * @param favourites All known favourites.
    */
  def updateFavourites(favourites: Seq[Favourite]): Unit
}

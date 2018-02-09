package dialogflow

import models._

import scala.concurrent.Future

/**
  * Created by alex on 21/01/18
  *
  * Upload entities to DialogFlow
  **/
trait UploadEntitiesService {

  /**
    * Upload all albums.
    * @param albums The albums to upload.
    * @return
    */
  def uploadAlbums(albums: Seq[Album]): Future[Unit]

  /**
    * Upload all artists.
    * @param artists The artists to upload.
    * @return
    */
  def uploadArtists(artists: Seq[Artist]): Future[Unit]

  /**
    * Upload all favourites.
    * @param favourites The favourites to upload.
    * @return
    */
  def uploadFavourites(favourites: Seq[Favourite]): Future[Unit]

  /**
    * Upload all playlists.
    * @param playlists The playlists to upload.
    * @return
    */
  def uploadPlaylists(playlists: Seq[Playlist]): Future[Unit]

  /**
    * Upload all rooms.
    * @param room The rooms to upload.
    * @return
    */
  def uploadRooms(room: Seq[Room]): Future[Unit]
}

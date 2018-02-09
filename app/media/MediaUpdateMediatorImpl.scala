package media

import javax.inject.Inject

import dialogflow.UploadEntitiesService
import models.{Album, Artist}
import play.api.Logger
import squeezebox.SqueezeCentre

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 26/12/17
  *
  * The default implementation of [[MediaUpdateMediator]].
  **/
class MediaUpdateMediatorImpl @Inject()(val mediaCache: MediaCache, val squeezeCentre: SqueezeCentre, val uploadEntitiesService: UploadEntitiesService)(implicit val ec: ExecutionContext) extends MediaUpdateMediator {

  private def log(message: String): Unit = {
    Logger.info(message)
  }


  implicit class Logging(message: String) {

    def >>[A](block: => A): A = {
      log(message)
      block
    }
  }

  override def update: Future[Unit] = {
    for {
      scRooms <- "Searching for rooms" >> squeezeCentre.rooms
      scAlbums <- "Searching for albums" >> squeezeCentre.albums
      scFavourites <- "Searching for favourites" >> squeezeCentre.favourites
      scPlaylists <- "Searching for playlists" >> squeezeCentre.playlists
      rooms <- "Caching rooms" >> Future.successful(mediaCache.updateRooms(scRooms.toSeq))
      albums <- "Caching albums" >> Future.successful(mediaCache.updateAlbums(scAlbums))
      artists <- "Caching artists" >> Future.successful(mediaCache.updateArtists(extractArtists(albums)))
      favourites <- "Caching favourites" >> Future.successful(mediaCache.updateFavourites(scFavourites.toSeq))
      playlists <- "Caching playlists" >> Future.successful(mediaCache.updatePlaylists(scPlaylists.toSeq))
      _ <- "Uploading albums" >> uploadEntitiesService.uploadAlbums(albums)
      _ <- "Uploading artists" >> uploadEntitiesService.uploadArtists(artists)
      _ <- "Uploading favourites" >> uploadEntitiesService.uploadFavourites(favourites)
      _ <- "Uploading playlists" >> uploadEntitiesService.uploadPlaylists(playlists)
      _ <- "Uploading rooms" >> uploadEntitiesService.uploadRooms(rooms)
    } yield {
      log("Updating complete")
    }
  }

  def extractArtists(albums: Seq[Album]): Seq[Artist] = {
    albums.flatMap(_.artists).distinct
  }
}

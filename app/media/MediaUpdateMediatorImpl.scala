package media

import javax.inject.Inject
import dialogflow.UploadEntitiesService
import models.{Album, Artist}
import play.api.Logger
import squeezebox.{MusicPlayer, MusicRepository, RoomsProvider}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 26/12/17
  *
  * The default implementation of [[MediaUpdateMediator]].
  **/
class MediaUpdateMediatorImpl @Inject()(
                                         val mediaCacheUpdater: MediaCacheUpdater,
                                         val musicRepository: MusicRepository,
                                         val uploadEntitiesService: UploadEntitiesService,
                                         val roomsProvider: RoomsProvider)(implicit val ec: ExecutionContext) extends MediaUpdateMediator {

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
      scRooms <- "Searching for rooms" >> roomsProvider.rooms()
      scAlbums <- "Searching for albums" >> musicRepository.albums
      scFavourites <- "Searching for favourites" >> musicRepository.favourites
      scPlaylists <- "Searching for playlists" >> musicRepository.playlists
      rooms <- "Caching rooms" >> Future.successful(mediaCacheUpdater.updateRooms(scRooms))
      albums <- "Caching albums" >> Future.successful(mediaCacheUpdater.updateAlbums(scAlbums))
      artists <- "Caching artists" >> Future.successful(mediaCacheUpdater.updateArtists(extractArtists(albums)))
      favourites <- "Caching favourites" >> Future.successful(mediaCacheUpdater.updateFavourites(scFavourites))
      playlists <- "Caching playlists" >> Future.successful(mediaCacheUpdater.updatePlaylists(scPlaylists))
      _ <- "Uploading albums" >> uploadEntitiesService.uploadAlbums(albums)
      _ <- "Uploading artists" >> uploadEntitiesService.uploadArtists(artists)
      _ <- "Uploading favourites" >> uploadEntitiesService.uploadFavourites(favourites)
      _ <- "Uploading playlists" >> uploadEntitiesService.uploadPlaylists(playlists)
      _ <- "Uploading rooms" >> uploadEntitiesService.uploadRooms(rooms)
    } yield {
      log("Updating complete")
    }
  }

  def extractArtists(albums: Set[Album]): Set[Artist] = {
    albums.flatMap(_.artists)
  }
}

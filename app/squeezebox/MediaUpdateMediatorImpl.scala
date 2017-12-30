package squeezebox

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 26/12/17
  **/
class MediaUpdateMediatorImpl @Inject() (val mediaCache: MediaCache, val squeezeCentre: SqueezeCentre)(implicit val ec: ExecutionContext) extends MediaUpdateMediator {

  override def update: Future[Unit] = {
    for {
      players <- squeezeCentre.players
      albums <- squeezeCentre.albums
      favourites <- squeezeCentre.favourites
    } yield {
      mediaCache.updatePlayers(players.toSeq)
      mediaCache.updateAlbums(albums)
      mediaCache.updateFavourites(favourites.toSeq)
    }
  }
}

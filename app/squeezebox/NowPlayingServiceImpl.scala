package squeezebox

import javax.inject.Inject

import hacks.AlbumTrackReportingReversalHack
import models.Room

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 31/01/18
  **/
class NowPlayingServiceImpl @Inject() (
                                        squeezeCentre: SqueezeCentre,
                                        albumTrackReportingReversalHack: AlbumTrackReportingReversalHack)
                                      (implicit ec: ExecutionContext)
  extends NowPlayingService {

  override def apply(room: Room): Future[Option[CurrentTrack]] = {
    squeezeCentre.playlistInformation(room).map { maybePlaylistInformation =>
      maybePlaylistInformation.map { playlistInformation =>
        val originalTitleAndArtist = (playlistInformation.title, playlistInformation.maybeArtist.getOrElse("Unknown"))
        val requiresReversal = playlistInformation.maybeRemoteTitle match {
          case Some(remoteTitle) => albumTrackReportingReversalHack.requiresReversal(remoteTitle)
          case None => false
        }
        val titleAndArtist = if (requiresReversal) originalTitleAndArtist.swap else originalTitleAndArtist
        CurrentTrack(titleAndArtist._1, titleAndArtist._2)
      }
    }
  }
}

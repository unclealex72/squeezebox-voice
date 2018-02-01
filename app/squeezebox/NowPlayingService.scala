package squeezebox

import models.Room

import scala.concurrent.Future

/**
  * Created by alex on 31/01/18
  *
  * A service used to find out what is currently playing on a squeezebox.
  **/
trait NowPlayingService {

  /**
    * Report on what is currently being played.
    * @param room The room of the squeezebox being queried.
    * @return Eventually the current track if something is playing, false otherwise.
    */
  def apply(room: Room): Future[Option[CurrentTrack]]
}

/**
  * A class to encapsulate the current track being played.
  * @param title The title of the song.
  * @param artist The song's artist.
  */
case class CurrentTrack(title: String, artist: String)
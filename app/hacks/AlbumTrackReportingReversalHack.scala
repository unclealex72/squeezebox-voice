package hacks

/**
  * Created by alex on 31/01/18
  *
  * Planet Rock reports tracks as artists and artists as tracks. This hack is used to work out whether
  * the track and artist need swapping when reporting on what is currently being played.
  **/
trait AlbumTrackReportingReversalHack {

  /**
    * Indicate whether tracks and albums need swapping.
    * @param playlistName The remote name of the playlist.
    * @return True if the track and album need swapping, false otherwise.
    */
  def requiresReversal(playlistName: String): Boolean
}

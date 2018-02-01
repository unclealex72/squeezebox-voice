package hacks

/**
  * Created by alex on 31/01/18
  *
  * An implementation of [[AlbumTrackReportingReversalHack]] that decides whether tracks and albums should be reversed
  * by using a whitelist of playlist names.
  **/
class SequenceMatchingAlbumTrackReportingReversalHack(playlists: Seq[String]) extends AlbumTrackReportingReversalHack {

  override def requiresReversal(playlistName: String): Boolean = {
    playlists.contains(playlistName)
  }
}

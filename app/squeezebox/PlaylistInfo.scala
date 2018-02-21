package squeezebox

/**
  * An object that encapsulates what is currently playing.
  * @param title The title of the track.
  * @param maybeArtist The artist, if known.
  * @param maybeRemoteTitle The remote title of the playlist if it is a remote stream or none otherwise.
  */
case class PlaylistInfo(title: String, maybeArtist: Option[String], maybeRemoteTitle: Option[String])

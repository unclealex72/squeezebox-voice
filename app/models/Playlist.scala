package models

/**
  * Created by alex on 26/12/17
  *
  * A class that encapsulates saved playlists stored on the SqueezeCentre.
  **/
case class Playlist(
                      /**
                        * The ID of the favourite.
                        */
                      id: String,
                      /**
                        * The name of the playlist.
                        */
                      name: String,
                      /**
                        * The URL for the playlist.
                        */
                      url : String,
                      /**
                        * The DialogFlow entry for the playlist.
                        */
                      entry: Entry)
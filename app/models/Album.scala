package models

/**
  * Created by alex on 23/12/17
  *
  * A class that encapsulates all albums on the SqueezeCentre that have the same title.
  **/
case class Album(
                  /**
                    * The title of the albums.
                    */
                  title: String,
                  /**
                    * The artists for the individual albums.
                    */
                  artists: Set[Artist],
                  /**
                    * The DialogFlow entry for these albums.
                    */
                  entry: Entry)
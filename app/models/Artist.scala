package models

/**
  * Created by alex on 23/12/17
  *
  * A class that encapsulates an artist.
  **/
case class Artist(
                   /**
                     * The name of the artist.
                     */
                   name: String,
                   /**
                     * A DialogFlow entry for the artist.
                     */
                   entry: Entry)
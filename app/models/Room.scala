package models

/**
  * Created by alex on 24/12/17
  *
  * A class that encapsulates a room that contains a Squeezebox.
  **/
case class Room(
                 /**
                   * The ID of the player in the room.
                   */
                 id: String,
                 /**
                   * The name of the room.
                   */
                 name: String,
                 /**
                   * The DialogFlow entry for this room.
                   */
                 entry: Entry)
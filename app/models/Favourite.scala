package models

/**
  * Created by alex on 26/12/17
  *
  * A class that encapsulates favourites stored on the SqueezeCentre.
  **/
case class Favourite(
                      /**
                        * The ID of the favourite.
                        */
                      id: String,
                      /**
                        * The name of the favourite.
                        */
                      name: String,
                      /**
                        * The DialogFlow entry for the favourite.
                        */
                      entry: Entry)
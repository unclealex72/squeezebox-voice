package squeezebox

import models.Room

import scala.concurrent.Future

/**
  * Provide all known rooms.
  * Created by alex on 24/02/18
  **/
trait RoomsProvider {

  /**
    * Get all known rooms.
    * @return All known rooms.
    */
  def rooms(): Future[Set[Room]]
}

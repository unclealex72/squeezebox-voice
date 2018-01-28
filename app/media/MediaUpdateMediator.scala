package media

import scala.concurrent.Future

/**
  * Make sure all required caches and DialogFlow entities are synchronised.
  * Created by alex on 26/12/17
  **/
trait MediaUpdateMediator {

  /**
    * Update caches and DialogFlow entities.
    * @return
    */
  def update: Future[Unit]
}

package squeezebox

import scala.concurrent.Future

/**
  * Make sure all required caches and external files are
  * Created by alex on 26/12/17
  **/
trait MediaUpdateMediator {

  def update: Future[Unit]
}

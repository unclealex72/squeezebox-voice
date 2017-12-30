package squeezebox

import scala.concurrent.Future

/**
  * A service to send commands to squeezeboxes.
  * Created by alex on 23/12/17
  **/
trait CommandService {

  /**
    * Execute a command on the logitech media server.
    * @param command The command to execute.
    * @return The eventual return value of the command.
    */
  def execute(command: String): Future[String]
}

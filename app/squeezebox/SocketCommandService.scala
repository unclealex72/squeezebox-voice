package squeezebox
import java.io._
import java.net.Socket
import java.nio.charset.StandardCharsets

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 23/12/17
  **/
class SocketCommandService(squeezeCentreLocation: SqueezeCentreLocation)(implicit ec: ExecutionContext) extends CommandService with StrictLogging {

  val port: Int = squeezeCentreLocation.port
  val host: String = squeezeCentreLocation.host

  /**
    * Execute a command on the logitech media server.
    *
    * @param command The command to execute.
    * @return The eventual return value of the command.
    */
  override def execute(command: String): Future[String] = Future {
    logger.info(s"Sending command to $host:$port - $command")
    val socket = new Socket(host, port)
    try {
      val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, StandardCharsets.UTF_8.name()))
      writer.write(command)
      writer.write('\n')
      writer.flush()
      val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, StandardCharsets.UTF_8.name()))
      reader.readLine()
    }
    finally {
      socket.close()
    }
  }
}
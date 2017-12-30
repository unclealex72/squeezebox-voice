package squeezebox
import java.io._
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.ActorSystem

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by alex on 23/12/17
  **/
class SocketCommandService(mediaCentre: MediaCentre)(implicit ec: ExecutionContext) extends CommandService {
  /**
    * Execute a command on the logitech media server.
    *
    * @param command The command to execute.
    * @return The eventual return value of the command.
    */
  override def execute(command: String): Future[String] = Future {
    val socket = new Socket(mediaCentre.host, mediaCentre.port)
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
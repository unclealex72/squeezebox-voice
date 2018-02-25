package squeezebox

import lexical.RemovePunctuationService
import models.{Entry, Room}

import scala.concurrent.Future

/**
  * A room provider that always returns the same rooms.
  * Created by alex on 24/02/18
  **/
class StaticRoomsProvider(
                           val roomNamesById: Map[String, String],
                           unpunctuated: RemovePunctuationService) extends RoomsProvider {

  private val _rooms: Future[Set[Room]] =
    Future.successful(roomNamesById.map {
      case(id, name) => Room.apply(id, name, Entry(unpunctuated(name), Seq.empty))
    }.toSet)

  def rooms(): Future[Set[Room]] = _rooms
}

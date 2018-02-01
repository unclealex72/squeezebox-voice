package squeezebox
import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import javax.inject.Inject

import lexical.{RemovePunctuationService, SynonymService}
import models._
import play.api.Logger

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * The default implementation of ``SqueezeCentre``
  * Created by alex on 24/12/17
  **/
class SqueezeCentreImpl @Inject()(commandService: CommandService, synonyms: SynonymService, removePunctuation: RemovePunctuationService)(implicit ec: ExecutionContext) extends SqueezeCentre {

  def execute(command: String): Future[Seq[(String, String)]] = {
    Logger.info(command)
    def parse(response: String): Seq[(String, String)] = {
      def splitByColon(str: String): Option[(String, String)] = {
        val maybeFirstColonPosition = Some(str.indexOf(':')).filter(_ != -1)
        maybeFirstColonPosition.map { firstColonPosition =>
          val (key, valueWithColon) = str.splitAt(firstColonPosition)
          val value = valueWithColon.drop(1)
          (key, value)
        }
      }
      val responseWithoutCommand = response.drop(command.length)
      for {
        encodedSegment <- responseWithoutCommand.trim().split("""\s+""")
        segment <- Some(dec(encodedSegment))
        pair <- splitByColon(segment)
      } yield pair
    }
    for {
      response <- commandService.execute(command)
    } yield {
      Logger.info(response)
      parse(response)
    }
  }

  def count(command: String): Future[Int] = {
    execute(command).map { keyValuePairs =>
      val maybeCount = for {
        keyValue <- keyValuePairs.headOption if keyValue._1 == "count"
        numericValue <- Try(keyValue._2.toInt).toOption
      } yield numericValue
      maybeCount.getOrElse(0)
    }
  }

  /**
    * Get a list of all known albums.
    *
    * @return A list of all known albums.
    */
  override def albums: Future[Seq[Album]] = {
    for {
      albumCount <- count("albums")
      response <- execute(s"albums 0 $albumCount tags:la")
    } yield parseAlbums(response)
  }

  def parseAlbums(response: Seq[(String, String)]): Seq[Album] = {
    case class ProtoAlbum(title: String, artist: String)
    val protoAlbums = for {
      map <- toMaps(response, "id")
      title <- map.get("album")
      artist <- map.get("artist")
    } yield {
      Logger.info(s"Found album $title by $artist")
      ProtoAlbum(title, artist)
    }
    protoAlbums.groupBy(_.title).toSeq.map {
      case (title, protos) =>
        val artists = protos.map { proto =>
          val artist = proto.artist
          Artist(artist, entryOf(artist))
        }
        Album(title, artists, entryOf(title))
    }
  }

  override def playlistInformation(room: Room): Future[Option[PlaylistInfo]] = {
    rooms.flatMap { availableRooms =>
      if (availableRooms.contains(room)) {
        execute(s"${room.id} status - 1 tags:Na").map { kvs =>
          val responseMap = kvs.toMap
          responseMap.get("title").map { title =>
            PlaylistInfo(title, responseMap.get("artist"), responseMap.get("remote_title"))
          }
        }
      }
      else {
        Future.successful(None)
      }
    }
  }

  def entryOf(name: String): Entry = {
    val unpunctuatedName = removePunctuation(name)
    Entry(unpunctuatedName, synonyms(unpunctuatedName))
  }

  def toMaps(response: Seq[(String, String)], delimitingKey: String): Seq[Map[String, String]] = {
    case class State(
                      subLists: Seq[Map[String, String]] = Seq.empty,
                      currentPartition: Map[String, String] = Map.empty)
    val finalState = response.foldLeft(State()) { (state, kv) =>
      val (key, value) = kv
      val (subLists, currentPartition) = if (key == delimitingKey) {
        (state.subLists :+ state.currentPartition, Map.empty[String, String])
      }
      else {
        (state.subLists, state.currentPartition)
      }
      State(subLists, currentPartition + (key -> value))
    }
    (finalState.subLists :+ finalState.currentPartition).filter(_.get(delimitingKey).isDefined)
  }

  def executeAndParse[A](command: String, separator: String, ordering: A => String)
                        (parser: Map[String, String] => Option[A])
                        (implicit ec: ExecutionContext): Future[SortedSet[A]] = {
    execute(command).map { responses =>
      val empty: SortedSet[A] = SortedSet.empty[A](Ordering.by(ordering(_).toLowerCase))
      empty ++ toMaps(responses, separator).flatMap(parser.apply)
    }
  }

  /**
    * Get a list of all connected players.
    *
    * @return A list of all known players.
    */
  override def rooms: Future[SortedSet[Room]] = {
    executeAndParse[Room]("players 0", "playerindex", _.name) { map =>
      for {
        id <- map.get("playerid")
        name <- map.get("name")
      } yield {
        Logger.info(s"Found room $name")
        Room(id, name, entryOf(name))
      }
    }
  }

  /**
    * Get a list of all known favourites.
    *
    * @return A list of all known favourites.
    */
  override def favourites: Future[SortedSet[Favourite]] = {
    executeAndParse[Favourite]("favorites items 0 200 tags:name", "id", _.name) { map =>
      for {
        id <- map.get("id")
        name <- map.get("name")
        isAudio <- map.get("isaudio") if isAudio == "1"
      } yield {
        Logger.info(s"Found favourite $name")
        Favourite(id, name, entryOf(name))
      }
    }
  }

  def executeAndIgnore(str: String): Future[Unit] = {
    execute(str).map(_ => {})
  }

  /**
    * Play an album on a player.
    *
    * @param player The player that will play the album.
    * @param album  The album to play.
    * @param artist The artist of the album
    * @return Unit.
    */
  override def playAlbum(player: Room, album: Album, artist: Artist): Future[Unit] = {
    executeAndIgnore(s"${player.id} playlist loadalbum * ${enc(artist.name)} ${enc(album.title)}")
  }

  /**
    * Play a favourite on a player.
    *
    * @param player    The player that will play the album.
    * @param favourite The favourite to play.
    * @return Unit.
    */
  override def playFavourite(player: Room, favourite: Favourite): Future[Unit] = {
    executeAndIgnore(s"${player.id} favorites playlist play item_id:${enc(favourite.id)}")
  }

  def enc(str: String): String =
    URLEncoder.encode(str, StandardCharsets.UTF_8.name()).replace("+", "%20")
  def dec(str: String): String =
    URLDecoder.decode(str.replace("%20", "+"), StandardCharsets.UTF_8.name())

}

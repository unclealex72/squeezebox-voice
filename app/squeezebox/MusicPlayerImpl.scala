package squeezebox
import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

import javax.inject.Singleton
import lexical.{RemovePunctuationService, SynonymService}
import models._
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * The default implementation of [[MusicPlayer]] and [[MusicRepository]]
  * Created by alex on 24/12/17
  **/
@Singleton
class MusicPlayerImpl(commandService: CommandService, synonymService: SynonymService, removePunctuationService: RemovePunctuationService)(implicit ec: ExecutionContext) extends MusicPlayer with MusicRepository {

  def execute(command: String): Future[Seq[(String, String)]] = {
    Logger.info(command)
    def parse(response: String): Seq[(String, String)] = {
      def splitByColon(str: String): Option[(String, String)] = {
        val maybeFirstColonPosition: Option[Int] = Some(str.indexOf(':')).filter(_ != -1)
        maybeFirstColonPosition.map { firstColonPosition =>
          val (key, valueWithColon) = str.splitAt(firstColonPosition)
          val value = valueWithColon.drop(1)
          (key, value)
        }
      }
      val responseWithoutCommand: String = response.drop(command.length)
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
      val maybeCount: Option[Int] = for {
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
  override def albums: Future[Set[Album]] = {
    for {
      albumCount <- count("albums")
      response <- execute(s"albums 0 $albumCount tags:la")
    } yield parseAlbums(response)
  }

  def parseAlbums(response: Seq[(String, String)]): Set[Album] = {
    case class ProtoAlbum(title: String, artist: String)
    val protoAlbums: Seq[ProtoAlbum] = for {
      map <- toMaps(response, "id")
      title <- map.get("album")
      artist <- map.get("artist")
    } yield {
      Logger.info(s"Found album $title by $artist")
      ProtoAlbum(title, artist)
    }
    protoAlbums.groupBy(_.title).toSeq.map {
      case (title, protos) =>
        val artists: Seq[Artist] = protos.map { proto =>
          val artist: String = proto.artist
          Artist(artist, entryOf(artist))
        }
        Album(title, artists.toSet, entryOf(title))
    }.toSet
  }

  override def playlistInformation(room: Room): Future[Option[PlaylistInfo]] = {
    connectedRooms.flatMap { availableRooms =>
      if (availableRooms.map(_.entry.unpunctuated).contains(room.entry.unpunctuated)) {
        execute(s"${room.id} status - 1 tags:Na").map { kvs =>
          val responseMap: Map[String, String] = kvs.toMap
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
    val unpunctuatedName = removePunctuationService(name)
    Entry(unpunctuatedName, synonymService(unpunctuatedName))
  }

  def toMaps(response: Seq[(String, String)], delimitingKey: String): Seq[Map[String, String]] = {
    case class State(
                      subLists: Seq[Map[String, String]] = Seq.empty,
                      currentPartition: Map[String, String] = Map.empty)
    val finalState: State = response.foldLeft(State()) { (state, kv) =>
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
                        (implicit ec: ExecutionContext): Future[Set[A]] = {
    execute(command).map { responses =>
      toMaps(responses, separator).flatMap(parser.apply).toSet
    }
  }

  /**
    * Get a list of all connected players.
    *
    * @return A list of all known players.
    */
  override def connectedRooms: Future[Set[Room]] = {
    executeAndParse[Room]("players 0", "playerindex", _.name) { map =>
      for {
        id <- map.get("playerid")
        name <- map.get("name")
        connected <- map.get("connected") if connected == "1"
      } yield {
        Logger.info(s"Found room $name with ID $id")
        Room(id, name, entryOf(name))
      }
    }
  }

  /**
    * Get a list of all known favourites.
    *
    * @return A list of all known favourites.
    */
  override def favourites: Future[Set[Favourite]] = {
    executeAndParse[Favourite]("favorites items 0 99999999 tags:name", "id", _.name) { map =>
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

  /**
    * Get a list of all known playlists.
    *
    * @return A list of all known playlists.
    */
  override def playlists: Future[Set[Playlist]] = {
    executeAndParse[Playlist]("playlists 0 99999999 tags:u", "id", _.name) { map =>
      for {
        id <- map.get("id")
        name <- map.get("playlist")
        url <- map.get("url")
      } yield {
        Logger.info(s"Found playlist $name")
        Playlist(id, name, url, entryOf(name))
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

  /**
    * Play a playlist on a player.
    *
    * @param player    The player that will play the album.
    * @param playlist The playlist to play.
    * @return Unit.
    */
  override def playPlaylist(player: Room, playlist: Playlist): Future[Unit] = {
    executeAndIgnore(s"${player.id} playlist play ${enc(playlist.url)} ${enc(playlist.name)}")
  }

  def enc(str: String): String =
    URLEncoder.encode(str, StandardCharsets.UTF_8.name()).replace("+", "%20")
  def dec(str: String): String =
    URLDecoder.decode(str.replace("%20", "+"), StandardCharsets.UTF_8.name())

}

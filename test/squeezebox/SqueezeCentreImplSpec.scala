package squeezebox

import lexical.{RemovePunctuationService, SynonymService}
import models.{Album, Artist, Favourite, Room}
import org.scalatest._

import scala.concurrent.Future

/**
  * Created by alex on 24/12/17
  **/
class SqueezeCentreImplSpec extends AsyncFlatSpec with Matchers {

  def squeezeCentre(maybePlaylist: Option[String] = None) =
    new SqueezeCentreImpl(new StaticCommandService(maybePlaylist), LowerCasingSynonymService, RemoveBracketsOnlyPunctuationService)

  val room = Room("01:02:03:04:05", "Kitchen", entryOf("Kitchen"))

  behavior of "squeezecentre"

  it should "eventually parse commands into pairs of strings" in {
    squeezeCentre().execute("players 0").map { responses =>
      responses should equal(Seq(
        "count" -> "2", "playerindex" -> "0", "playerid" -> "00:01:02:03:04:05", "uuid" -> "",
        "ip" -> "10.5.6.28:16660", "name" -> "Kitchen", "seq_no" -> "0", "model" -> "squeezebox2",
        "modelname" -> "Squeezebox2", "power" -> "1", "isplaying" -> "1", "displaytype" -> "graphic-320x32",
        "isplayer" -> "1", "canpoweroff" -> "1", "connected" -> "1", "firmware" -> "137", "playerindex" -> "1",
        "playerid" -> "80:81:82:83:84:85", "uuid" -> "", "ip" -> "10.7.9.200:40841", "name" -> "Bedroom",
        "seq_no" -> "0", "model" -> "squeezebox3", "modelname" -> "Squeezebox Classic", "power" -> "1",
        "isplaying" -> "0", "displaytype" -> "graphic-320x32", "isplayer" -> "1", "canpoweroff" -> "1",
        "connected" -> "1", "firmware" -> "137"
      ))
    }
  }

  it should "eventually get a list of all known players" in {
    squeezeCentre().rooms.map { players =>
      players should contain only(
        Room("80:81:82:83:84:85", "Bedroom", entryOf("Bedroom")),
        Room("00:01:02:03:04:05", "Kitchen", entryOf("Kitchen"))
      )
    }
  }

  it should "eventually get a list of all known favourites" in {
    squeezeCentre().favourites.map { favourites =>
      favourites should contain only Favourite("ee3fea76.1", "Planet Rock", entryOf("Planet Rock"))
    }
  }

  it should "eventually list all albums" in {
    val queen = Artist("Queen", entryOf("Queen"))
    val thePolice = Artist("The Police", entryOf("The Police"))
    squeezeCentre().albums.map { albums =>
      albums should contain only(
        Album("A Kind of Magic", Set(queen), entryOf("A Kind of Magic")),
        Album("A Kind of Magic (Extras)", Set(queen), entryOf("A Kind of Magic Extras")),
        Album("A Night at the Opera", Set(queen), entryOf("A Night at the Opera")),
        Album("Greatest Hits", Set(queen, thePolice), entryOf("Greatest Hits"))
      )
    }
  }

  it should "report that nothing is playing" in {
    squeezeCentre(Playlists.nothing).playlistInformation(room).map { maybePlaylistInformation =>
      maybePlaylistInformation should equal(None)
    }
  }

  it should "report that that Jimi Hendrix is playing on Planet Rock" in {
    squeezeCentre(Playlists.planetRock).playlistInformation(room).map { maybePlaylistInformation =>
      maybePlaylistInformation should equal(
        Some(PlaylistInfo("All Along The Watchtower", Some("Jimi Hendrix Experience"), Some("Planet Rock"))))
    }
  }

  it should "report that that Pet Sounds is playing" in {
    squeezeCentre(Playlists.petSounds).playlistInformation(room).map { maybePlaylistInformation =>
      maybePlaylistInformation should equal(
        Some(PlaylistInfo("Wouldn't It Be Nice", Some("The Beach Boys"), None)))
    }
  }

  private def entryOf(name: String): models.Entry = models.Entry(name, Seq(name.toLowerCase))
}

object LowerCasingSynonymService extends SynonymService {
  override def apply(str: String): Seq[String] = Seq(str.toLowerCase)
}

object RemoveBracketsOnlyPunctuationService extends RemovePunctuationService {
  override def apply(str: String): String = str.replaceAll("""[\(\)]""", "")
}

class StaticCommandService(maybePlaylist: Option[String]) extends CommandService {
  val requestsAndResponses: Map[String, String] = Map(
    "players 0" ->
      """players 0  count%3A2
        |playerindex%3A0 playerid%3A00%3A01%3A02%3A03%3A04%3A05 uuid%3A ip%3A10.5.6.28%3A16660 name%3AKitchen
        |seq_no%3A0 model%3Asqueezebox2 modelname%3ASqueezebox2 power%3A1 isplaying%3A1 displaytype%3Agraphic-320x32
        |isplayer%3A1 canpoweroff%3A1 connected%3A1 firmware%3A137 playerindex%3A1
        |playerid%3A80%3A81%3A82%3A83%3A84%3A85 uuid%3A ip%3A10.7.9.200%3A40841 name%3ABedroom seq_no%3A0
        |model%3Asqueezebox3 modelname%3ASqueezebox%20Classic power%3A1 isplaying%3A0 displaytype%3Agraphic-320x32
        |isplayer%3A1 canpoweroff%3A1 connected%3A1 firmware%3A137""".stripMargin,
    "albums" -> "albums   count%3A5",
    "favorites items 0 99999999 tags:name" ->
      """favorites items 0 99999999 tags%3Aname title%3AFavorites
        |id%3Aee3fea76.0 name%3AOn%20mysqueezebox.com isaudio%3A0 hasitems%3A1
        |id%3Aee3fea76.1 name%3APlanet%20Rock type%3Aaudio isaudio%3A1 hasitems%3A0 count%3A2""".stripMargin,
    "albums 0 5 tags:la" ->
      """albums 0 5 tags%3Ala
        |id%3A4482 album%3AA%20Kind%20of%20Magic artist%3AQueen
        |id%3A4481 album%3AA%20Kind%20of%20Magic%20(Extras) artist%3AQueen
        |id%3A4074 album%3AGreatest%20Hits artist%3AQueen
        |id%3A4484 album%3AA%20Night%20at%20the%20Opera artist%3AQueen
        |id%3A4483 album%3AGreatest%20Hits artist%3AThe%20Police""".stripMargin) ++
    maybePlaylist.map("01:02:03:04:05 status - 1 tags:Na" -> _)


  override def execute(command: String): Future[String] =
    Future.successful(requestsAndResponses.getOrElse(command, command))
}

object Playlists {
  val planetRock: Option[String] = Some(
    """00%3A01%3A02%3A03%3A04%3A05 status - 1 tags%3ANa
      |player_name%3ABedroom player_connected%3A1 player_ip%3A192.168.1.81%3A41101
      |power%3A1 signalstrength%3A0 mode%3Aplay remote%3A1 current_title%3APlanet%20Rock
      |time%3A326.048606931686 rate%3A1 mixer%20volume%3A100 playlist%20repeat%3A0
      |playlist%20shuffle%3A0 playlist%20mode%3Aoff seq_no%3A0 playlist_cur_index%3A0
      |playlist_timestamp%3A1517423735.80875 playlist_tracks%3A1 digital_volume_control%3A0
      |remoteMeta%3AHASH(0xc048c70) playlist%20index%3A0 id%3A-200136208
      |title%3AAll%20Along%20The%20Watchtower remote_title%3APlanet%20Rock
      |artist%3AJimi%20Hendrix%20Experience""".stripMargin)

  val petSounds: Option[String] = Some(
    """00%3A01%3A02%3A03%3A04%3A05 status - 1 tags%3ANa
      |player_name%3ABedroom player_connected%3A1 player_ip%3A192.168.1.81%3A41102
      |showBriefly%3ANow%20Playing%20(1%20of%2014)%20%2CWouldn't%20It%20Be%20Nice power%3A1 signalstrength%3A0
      |mode%3Aplay time%3A6.92212028884888 rate%3A1 duration%3A153.226 can_seek%3A1 mixer%20volume%3A100
      |playlist%20repeat%3A0 playlist%20shuffle%3A0 playlist%20mode%3Aoff seq_no%3A0 playlist_cur_index%3A0
      |playlist_timestamp%3A1517424114.07508 playlist_tracks%3A14 digital_volume_control%3A0 playlist%20index%3A0
      |id%3A61344 title%3AWouldn't%20It%20Be%20Nice artist%3AThe%20Beach%20Boys""".stripMargin)

  val nothing: Option[String] = Some(
    """00%3A01%3A02%3A03%3A04%3A05 status - 1 tags%3ANa
      |player_name%3ABedroom player_connected%3A1 player_ip%3A192.168.1.81%3A41105 power%3A1 signalstrength%3A0
      |mode%3Astop mixer%20volume%3A100 playlist%20repeat%3A0 playlist%20shuffle%3A0 playlist%20mode%3Aoff seq_no%3A0
      |playlist_tracks%3A0 digital_volume_control%3A0""".stripMargin)
}
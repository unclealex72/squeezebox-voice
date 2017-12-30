package squeezebox

import org.scalatest._

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 24/12/17
  **/
class SqueezeCentreImplSpec extends AsyncFlatSpec with Matchers {

  val squeezeCentre = new SqueezeCentreImpl(StaticCommandService, LowerCasingSynonymService)

  behavior of "squeezecentre"

  it should "eventually parse commands into pairs of strings" in {
    squeezeCentre.execute("players 0").map { responses =>
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
    squeezeCentre.players.map { players =>
      players should contain inOrderOnly(
        Player("80:81:82:83:84:85", "Bedroom"),
        Player("00:01:02:03:04:05", "Kitchen")
      )
    }
  }

  it should "eventually get a list of all known favourites" in {
    squeezeCentre.favourites.map { favourites =>
      favourites should contain only Favourite("ee3fea76.1", "Planet Rock")
    }
  }

  it should "eventually list all albums" in {
    squeezeCentre.albums.map { albums =>
      albums should contain only(
        Album("A Kind of Magic", "A Kind of Magic", SortedSet("Queen"), Seq("a kind of magic")),
        Album("A Kind of Magic (Extras)", "A Kind of Magic Extras", SortedSet("Queen"), Seq("a kind of magic (extras)")),
        Album("A Night at the Opera", "A Night at the Opera", SortedSet("Queen"), Seq("a night at the opera")),
        Album("Greatest Hits", "Greatest Hits", SortedSet("Queen", "The Police"), Seq("greatest hits"))
      )
    }
  }
}

object LowerCasingSynonymService extends SynonymService {
  override def synonyms(str: String): Seq[String] = Seq(str.toLowerCase)
}

object StaticCommandService extends CommandService {
  val requestsAndResponses = Map(
    "players 0" -> "players 0  count%3A2 playerindex%3A0 playerid%3A00%3A01%3A02%3A03%3A04%3A05 uuid%3A ip%3A10.5.6.28%3A16660 name%3AKitchen seq_no%3A0 model%3Asqueezebox2 modelname%3ASqueezebox2 power%3A1 isplaying%3A1 displaytype%3Agraphic-320x32 isplayer%3A1 canpoweroff%3A1 connected%3A1 firmware%3A137 playerindex%3A1 playerid%3A80%3A81%3A82%3A83%3A84%3A85 uuid%3A ip%3A10.7.9.200%3A40841 name%3ABedroom seq_no%3A0 model%3Asqueezebox3 modelname%3ASqueezebox%20Classic power%3A1 isplaying%3A0 displaytype%3Agraphic-320x32 isplayer%3A1 canpoweroff%3A1 connected%3A1 firmware%3A137",
    "albums" -> "albums   count%3A5",
    "favorites items 0 200 tags:name" ->
      """favorites items 0 200 tags%3Aname title%3AFavorites
        |id%3Aee3fea76.0 name%3AOn%20mysqueezebox.com isaudio%3A0 hasitems%3A1
        |id%3Aee3fea76.1 name%3APlanet%20Rock type%3Aaudio isaudio%3A1 hasitems%3A0 count%3A2""".stripMargin,
    "albums 0 5 tags:la" ->
      """albums 0 5 tags%3Ala
        |id%3A4482 album%3AA%20Kind%20of%20Magic artist%3AQueen
        |id%3A4481 album%3AA%20Kind%20of%20Magic%20(Extras) artist%3AQueen
        |id%3A4074 album%3AGreatest%20Hits artist%3AQueen
        |id%3A4484 album%3AA%20Night%20at%20the%20Opera artist%3AQueen
        |id%3A4483 album%3AGreatest%20Hits artist%3AThe%20Police""".stripMargin)


  override def execute(command: String): Future[String] =
    Future.successful(requestsAndResponses.getOrElse(command, command))
}

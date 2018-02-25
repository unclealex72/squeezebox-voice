package modules

import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}

import com.google.inject.Provider
import hacks.{AlbumTrackReportingReversalHack, SequenceMatchingAlbumTrackReportingReversalHack}
import play.api.Configuration
import play.api.inject.{SimpleModule, _}
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Created by alex on 26/12/17
  *
  **/
class HacksModule extends SimpleModule(
  bind[AlbumTrackReportingReversalHack].toProvider[AlbumTrackReportingReversalHackProvider]
)
@Singleton
class AlbumTrackReportingReversalHackProvider @Inject() (config: Configuration) extends Provider[AlbumTrackReportingReversalHack] {
  override def get(): AlbumTrackReportingReversalHack = {
    val hacks = Hacks(config)
    new SequenceMatchingAlbumTrackReportingReversalHack(hacks.albumArtistSwaps)
  }
}

case class Hacks(albumArtistSwaps: Seq[String])

object Hacks {

  implicit val hacksReads: Reads[Hacks] = {
    (JsPath \ "albumArtistSwaps").read[Seq[String]].map(Hacks(_))
  }

  def apply[A](config: Configuration): Hacks = {
    val hacksFile: String = config.get[String]("hacks.path")
    val in = new FileInputStream(hacksFile)
    try {
      Json.parse(in).as[Hacks]
    }
    finally {
      in.close()
    }
  }
}


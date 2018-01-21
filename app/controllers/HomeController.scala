package controllers

import javax.inject._

import cats.data._
import cats.instances.future._
import play.api.libs.json._
import play.api.mvc._
import squeezebox._

import scala.concurrent.{ExecutionContext, Future}
import cats.data._
import cats.syntax._
import cats.implicits._
import cats.instances._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, mediaUpdateMediator: MediaUpdateMediator, mediaCache: MediaCache, squeezeCentre: SqueezeCentre)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def update(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mediaUpdateMediator.update.map(_ => Ok(JsString("Updated")))
  }

  def play(): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request: Request[JsValue] =>
    val body = request.body

    val vAlbum: ValidatedNel[String, Album] = {
      val vAlbumTitle: ValidatedNel[String, String] =
        (body \ "album").asOpt[String].toRight("Please supply an album title").toValidatedNel
      vAlbumTitle.andThen { albumTitle: String =>
        mediaCache.album(albumTitle).toRight(s"Cannot find album $albumTitle").toValidatedNel
      }
    }

    val vRoom: ValidatedNel[String, Player] = {
      val vRoomName: ValidatedNel[String, String] =
        (body \ "room").asOpt[String].toRight("Please supply a room name").toValidatedNel
      vRoomName.andThen { roomName: String =>
        mediaCache.player(roomName).toRight(s"Cannot find room $roomName").toValidatedNel
      }
    }

    val vArtist: ValidatedNel[String, String] = vAlbum.andThen { album =>
      val artists = album.artists
      if (artists.size == 1) {
        artists.head.valid
      }
      else {
        val maybeArtistName = (body \ "artist").asOpt[String]
        val maybeArtist = maybeArtistName.flatMap(artistName => artists.find(_ == artistName))
        maybeArtist.toRight(
          s"Please supply a valid artist for album $album. One of: ${artists.mkString(", ")}").toValidatedNel
      }
    }

    val happyPath: ValidatedNel[String, Future[Unit]] = (vAlbum, vArtist, vRoom).mapN { (album: Album, artist: String, room: Player) =>
      squeezeCentre.playAlbum(room, album, artist)
    }
    happyPath match {
      case Validated.Valid(u) => u.map(_ => Ok("playing"))
      case Validated.Invalid(msgs) => Future.successful {
        BadRequest(msgs.toList.mkString("\n"))
      }
    }
  }

}

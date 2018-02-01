package webhook
import javax.inject.Inject

import cats.data.Validated._
import cats.data._
import cats.implicits._
import media.MediaCache
import models.{Album, Artist, Room}
import squeezebox.{NowPlayingService, SqueezeCentre}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 28/01/18
  *
  * The default implementation of [[WebhookService]]
  **/
class WebhookServiceImpl @Inject() (
                                     squeezeCentre: SqueezeCentre,
                                     mediaCache: MediaCache,
                                     nowPlaying: NowPlayingService)
                                   (implicit ec: ExecutionContext) extends WebhookService {

  type EventualResponse = Future[ValidatedNel[String, WebhookResponse]]

  val actions: Map[String, (Room, WebhookParameters) => EventualResponse] = Map(
    "play-favourite" -> playFavourite,
    "play-album" -> playAlbum,
    "provide-required-artist" -> playAlbum,
    "now-playing" -> currentTrack
  )

  override def apply(webhookRequest: WebhookRequest): EventualResponse = {
    val actionName = webhookRequest.action
    actions.get(actionName) match {
      case Some(action) => respond(webhookRequest.parameters, action)
      case None =>
        Future.successful(s"Received unknown action $actionName".invalidNel)
    }

  }

  def respond(
               parameters: WebhookParameters,
               action: (Room, WebhookParameters) => EventualResponse): EventualResponse = {
    parameters.room(mediaCache) match {
      case Valid(room) =>
        squeezeCentre.rooms.flatMap { availableRooms =>
          availableRooms.find(_.name.equalsIgnoreCase(room.name)) match {
            case Some(theRoom) => action(theRoom, parameters)
            case None => Future.successful(followup("room-not-connected", parameters).validNel)
          }
        }
      case Invalid(errs) => Future.successful(Invalid(errs))
    }
  }

  def playFavourite(room: Room, parameters: WebhookParameters): EventualResponse = {
    parameters.favourite(mediaCache) match {
      case Valid(favourite) =>
        squeezeCentre.playFavourite(room, favourite).map { _ =>
          followup("playing-favourite", parameters).validNel
        }
      case Invalid(errs) => Future.successful(Invalid(errs))
    }
  }

  def currentTrack(room: Room, parameters: WebhookParameters): EventualResponse = {
    nowPlaying(room).map {
      case Some(currentTrack) => {
        val title = currentTrack.title
        val artist = currentTrack.artist
        followup("currently-playing", parameters ++ ("currentTitle" -> title, "currentArtist" -> artist)).validNel
      }
      case None =>
        followup("nothing-playing", parameters).validNel
    }
  }

  def playAlbum(room: Room, parameters: WebhookParameters): EventualResponse = {
    case class AlbumAndMaybeArtist(album: Album, maybeArtist: Option[Artist]) {}
    val validatedAlbumAndMaybeArtist: ValidatedNel[String, AlbumAndMaybeArtist] =
      (parameters.album(mediaCache), parameters.maybeArtist(mediaCache)).mapN { AlbumAndMaybeArtist(_, _) }

    validatedAlbumAndMaybeArtist match {
      case Valid(AlbumAndMaybeArtist(album, maybeArtist)) =>
        val artists = album.artists.toList
        (artists, maybeArtist) match {
          case (artist :: Nil, None) => playSqueezeboxAlbum(room, album, artist, parameters).map(_.validNel)
          case (albumArtists, Some(artist)) => if (albumArtists.contains(artist)) {
            playSqueezeboxAlbum(room, album, artist, parameters).map(_.validNel)
          }
          else {
            Future.successful(followup("wrong-artist", parameters).validNel)
          }
          case (albumArtists, None) => artistRequired(albumArtists, parameters).map(_.validNel)
        }
      case Invalid(errs) => Future.successful(Invalid(errs))
    }
  }

  def playSqueezeboxAlbum(room: Room, album: Album, artist: Artist, parameters: WebhookParameters): Future[WebhookResponse] = {
    squeezeCentre.playAlbum(room, album, artist).map { _ =>
      followup("playing-album", parameters + ("artist" -> artist.name))
    }
  }

  def artistRequired(albumArtists: Seq[Artist], parameters: WebhookParameters): Future[WebhookResponse] = Future.successful {
    val artistNames = albumArtists.map(_.name).sorted.mkString(", ")
    followup("artist-required", parameters + ("artists" -> artistNames), Seq("artist-required-context"))
  }

  def followup(event: String, parameters: WebhookParameters, contextNames: Seq[String] = Seq.empty): WebhookResponse = {
    WebhookResponse(event, parameters, contextNames)
  }

}

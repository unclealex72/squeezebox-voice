package webhook
import javax.inject.Inject

import cats.data.Validated._
import cats.data._
import cats.implicits._
import media.{MediaCache, MediaUpdateMediator}
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
                                     nowPlaying: NowPlayingService,
                                     mediaUpdateMediator: MediaUpdateMediator)
                                   (implicit ec: ExecutionContext) extends WebhookService {

  type EventualResponse = Future[ValidatedNel[String, WebhookResponse]]

  import WebhookServiceImplicits._


  val actions: Map[String, WebhookParameters => EventualResponse] = Map(
    "play-favourite" -> withRoom(playFavourite),
    "play-playlist" -> withRoom(playPlaylist),
    "play-album" -> withRoom(playAlbum),
    "provide-required-artist" -> withRoom(playAlbum),
    "now-playing" -> withRoom(currentTrack),
    "browse-artist" -> browseArtist,
    "update" -> update
  )

  override def apply(webhookRequest: WebhookRequest): EventualResponse = {
    val actionName = webhookRequest.action
    actions.get(actionName) match {
      case Some(action) => action(webhookRequest.parameters)
      case None =>
        Future.successful(s"Received unknown action $actionName".invalidNel)
    }

  }

  implicit def withRoom(action: (Room, WebhookParameters) => EventualResponse)(parameters: WebhookParameters): EventualResponse = {
    parameters.room(mediaCache) ~> { room =>
      squeezeCentre.rooms.flatMap { availableRooms =>
        availableRooms.find(_.name.equalsIgnoreCase(room.name)) match {
          case Some(theRoom) => action(theRoom, parameters)
          case None => followup("room-not-connected", parameters)
        }
      }
    }
  }

  def playFavourite(room: Room, parameters: WebhookParameters): EventualResponse = {
    parameters.favourite(mediaCache) ~> { favourite =>
      squeezeCentre.playFavourite(room, favourite).map { _ =>
        followup("playing-favourite", parameters)
      }
    }
  }

  def playPlaylist(room: Room, parameters: WebhookParameters): EventualResponse = {
    parameters.playlist(mediaCache) ~> { playlist =>
      squeezeCentre.playPlaylist(room, playlist).map { _ =>
        followup("playing-playlist", parameters)
      }
    }
  }

  def currentTrack(room: Room, parameters: WebhookParameters): EventualResponse = {
    nowPlaying(room).map {
      case Some(currentTrack) =>
        val title = currentTrack.title
        val artist = currentTrack.artist
        followup("currently-playing", parameters ++ ("currentTitle" -> title, "currentArtist" -> artist))
      case None =>
        followup("nothing-playing", parameters)
    }
  }

  def playAlbum(room: Room, parameters: WebhookParameters): EventualResponse = {
    case class AlbumAndMaybeArtist(album: Album, maybeArtist: Option[Artist]) {}
    val validatedAlbumAndMaybeArtist: ValidatedNel[String, AlbumAndMaybeArtist] =
      (parameters.album(mediaCache), parameters.maybeArtist(mediaCache)).mapN {
        AlbumAndMaybeArtist(_, _)
      }

    validatedAlbumAndMaybeArtist ~> {
      case AlbumAndMaybeArtist(album, maybeArtist) =>
        val artists = album.artists.toList
        (artists, maybeArtist) match {
          case (artist :: Nil, None) => playSqueezeboxAlbum(room, album, artist, parameters)
          case (albumArtists, Some(artist)) => if (albumArtists.contains(artist)) {
            playSqueezeboxAlbum(room, album, artist, parameters)
          }
          else {
            followup("wrong-artist", parameters)
          }
          case (albumArtists, None) => artistRequired(albumArtists, parameters)
        }
    }
  }

  def browseArtist(parameters: WebhookParameters): EventualResponse = {
    parameters.artist(mediaCache) ~> { artist =>
      val albumNames = mediaCache.listAlbums(artist).map(_.title).sorted
      val albumList = albumNames.mkString(", ")
      followup("albums-for-artist", parameters + ("albums" -> albumList))
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

  def update(parameters: WebhookParameters): EventualResponse = {
    mediaUpdateMediator.update
    followup("updating", parameters)
  }

  def followup(event: String, parameters: WebhookParameters, contextNames: Seq[String] = Seq.empty): WebhookResponse = {
    WebhookResponse(event, parameters, contextNames)
  }

  private object WebhookServiceImplicits {

    implicit class EventualResponseHelpers[A](validatedValue: ValidatedNel[String, A]) {
      def ~>(eventualFactory: A => EventualResponse): EventualResponse = {
        validatedValue match {
          case Valid(a) => eventualFactory(a)
          case Invalid(errs) => Future.successful(Invalid(errs))
        }
      }
    }

    implicit val response: Future[WebhookResponse] => EventualResponse = { eResponse =>
      eResponse.map(_.validNel)
    }

    implicit val eResponse: ValidatedNel[String, WebhookResponse] => EventualResponse = { response =>
      Future.successful(response)
    }

    implicit val evResponse: WebhookResponse => EventualResponse = { response =>
      eResponse(response.validNel)
    }

    implicit val vResponse: WebhookResponse => ValidatedNel[String, WebhookResponse] = { response =>
      response.validNel
    }

  }

}
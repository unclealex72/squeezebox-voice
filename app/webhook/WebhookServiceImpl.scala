package webhook
import javax.inject.Inject

import cats.data.Validated._
import cats.data._
import cats.implicits._
import media.{MediaCacheView, MediaUpdateMediator}
import models.{Album, Artist, Room}
import squeezebox.{NowPlayingService, MusicPlayer}

import scala.concurrent.{ExecutionContext, Future}
import Action._
import Event._
/**
  * Created by alex on 28/01/18
  *
  * The default implementation of [[WebhookService]]
  **/
class WebhookServiceImpl @Inject() (
                                     musicPlayer: MusicPlayer,
                                     mediaCacheView: MediaCacheView,
                                     nowPlaying: NowPlayingService,
                                     mediaUpdateMediator: MediaUpdateMediator)
                                   (implicit ec: ExecutionContext) extends WebhookService {

  type EventualResponse = Future[ValidatedNel[String, WebhookResponse]]

  import WebhookServiceImplicits._

  val actions: Map[Action, WebhookParameters => EventualResponse] = Map(
    PlayFavourite -> withRoom(playFavourite),
    PlayPlaylist -> withRoom(playPlaylist),
    PlayAlbum -> withRoom(playAlbum),
    ProvideRequiredArtist -> withRoom(playAlbum),
    NowPlaying -> withRoom(currentTrack),
    BrowseArtist -> browseArtist,
    Update -> update
  )

  override def apply(webhookRequest: WebhookRequest): EventualResponse = {
    val action: Action = webhookRequest.action
    actions.get(action) match {
      case Some(factory) => factory(webhookRequest.parameters)
      case None =>
        Future.successful(s"Received unknown action ${action.action}".invalidNel)
    }

  }

  implicit def withRoom(action: (Room, WebhookParameters) => EventualResponse)(parameters: WebhookParameters): EventualResponse = {
    parameters.room(mediaCacheView) ~> { room =>
      musicPlayer.connectedRooms().flatMap { availableRooms =>
        availableRooms.find(_.name.equalsIgnoreCase(room.name)) match {
          case Some(theRoom) => action(theRoom, parameters)
          case None => followup(RoomNotConnected, parameters)
        }
      }
    }
  }

  def playFavourite(room: Room, parameters: WebhookParameters): EventualResponse = {
    parameters.favourite(mediaCacheView) ~> { favourite =>
      musicPlayer.playFavourite(room, favourite).map { _ =>
        followup(PlayingFavourite, parameters)
      }
    }
  }

  def playPlaylist(room: Room, parameters: WebhookParameters): EventualResponse = {
    parameters.playlist(mediaCacheView) ~> { playlist =>
      musicPlayer.playPlaylist(room, playlist).map { _ =>
        followup(PlayingPlaylist, parameters)
      }
    }
  }

  def currentTrack(room: Room, parameters: WebhookParameters): EventualResponse = {
    nowPlaying(room).map {
      case Some(currentTrack) =>
        followup(
          CurrentlyPlaying,
          parameters.withCurrentTitle(currentTrack.title).withCurrentArtist(currentTrack.artist))
      case None =>
        followup(NothingPlaying, parameters)
    }
  }

  def playAlbum(room: Room, parameters: WebhookParameters): EventualResponse = {
    case class AlbumAndMaybeArtist(album: Album, maybeArtist: Option[Artist]) {}
    val validatedAlbumAndMaybeArtist: ValidatedNel[String, AlbumAndMaybeArtist] =
      (parameters.album(mediaCacheView), parameters.maybeArtist(mediaCacheView)).mapN {
        AlbumAndMaybeArtist(_, _)
      }

    validatedAlbumAndMaybeArtist ~> {
      case AlbumAndMaybeArtist(album, maybeArtist) =>
        val artists: List[Artist] = album.artists.toList
        (artists, maybeArtist) match {
          case (artist :: Nil, None) => playSqueezeboxAlbum(room, album, artist, parameters)
          case (albumArtists, Some(artist)) => if (albumArtists.contains(artist)) {
            playSqueezeboxAlbum(room, album, artist, parameters)
          }
          else {
            followup(WrongArtist, parameters)
          }
          case (albumArtists, None) => artistRequired(albumArtists, parameters)
        }
    }
  }

  def browseArtist(parameters: WebhookParameters): EventualResponse = {
    parameters.artist(mediaCacheView) ~> { artist =>
      followup(AlbumsForArtist, parameters.withAlbums(mediaCacheView.listAlbums(artist).toSeq.sortBy(_.title)))
    }
  }

  def playSqueezeboxAlbum(room: Room, album: Album, artist: Artist, parameters: WebhookParameters): Future[WebhookResponse] = {
    musicPlayer.playAlbum(room, album, artist).map { _ =>
      followup(PlayingAlbum, parameters.withArtist(artist))
    }
  }

  def artistRequired(albumArtists: Seq[Artist], parameters: WebhookParameters): Future[WebhookResponse] = Future.successful {
    followup(ArtistRequired, parameters.withArtists(albumArtists.sortBy(_.name)), Seq(Context.ArtistRequired))
  }

  def update(parameters: WebhookParameters): EventualResponse = {
    mediaUpdateMediator.update
    followup(Updating, parameters)
  }

  def followup(event: Event, parameters: WebhookParameters, contexts: Seq[Context] = Seq.empty): WebhookResponse = {
    WebhookResponse(event, parameters, contexts)
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
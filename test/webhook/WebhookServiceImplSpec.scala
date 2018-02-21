package webhook

import cats.data.Validated.Valid
import media.{MediaCacheView, MediaUpdateMediator}
import models._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest._
import squeezebox.{CurrentTrack, MusicPlayer, NowPlayingService}

import scala.concurrent.Future

/**
  * Created by alex on 11/02/18
  **/
class WebhookServiceImplSpec extends AsyncFlatSpec with Matchers with OneInstancePerTest with Data {

  behavior of "webhook service"

  it should "play a favourite on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Favourite -> planetRock.name, Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.PlayFavourite, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playFavourite _).verify(kitchen, planetRock)
        response shouldBe Valid(WebhookResponse(Event.PlayingFavourite, parameters))
    }
  }

  it should "reject playing a favourite on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Favourite -> planetRock.name, Parameter.Room -> bedroom.name)
    val request = WebhookRequest(Action.PlayFavourite, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "play an album on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Album -> deathMagnetic.title, Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playAlbum _).verify(kitchen, deathMagnetic, metallica)
        response shouldBe Valid(
          WebhookResponse(
            Event.PlayingAlbum,
            parameters + (Parameter.Artist -> metallica.name)))
    }
  }

  it should "reject playing an album on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Album -> deathMagnetic.title, Parameter.Room -> bedroom.name)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "play a playlist item on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Playlist -> mesmerizeAndHypnotize.name, Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.PlayPlaylist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playPlaylist _).verify(kitchen, mesmerizeAndHypnotize)
        response shouldBe Valid(
          WebhookResponse(Event.PlayingPlaylist, parameters))
    }
  }

  it should "reject playing a playlist item on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Playlist -> mesmerizeAndHypnotize.name, Parameter.Room -> bedroom.name)
    val request = WebhookRequest(Action.PlayPlaylist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "query which artist for an album with more than one artist" in {
    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Album -> greatestHits.title, Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        response shouldBe Valid(
          WebhookResponse(
            Event.ArtistRequired,
            parameters + (Parameter.Artists -> "Queen, The Police"),
            Seq(Context.ArtistRequired)))
    }
  }

  it should "play an album when both an album and and artist are provided" in {
    val s: Services = createServices()
    val parameters = WebhookParameters(
      Parameter.Album -> greatestHits.title,
      Parameter.Artist -> queen.name,
      Parameter.Artists -> "Queen, The Police",
      Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playAlbum _).verify(kitchen, greatestHits, queen)
        response shouldBe Valid(
          WebhookResponse(
            Event.PlayingAlbum,
            parameters ++ (Parameter.Artist -> queen.name, Parameter.Artists -> "Queen, The Police")))
    }
  }

  it should "say what is playing on a room that is connected and currently playing something" in {
    val s: Services = createServices()
    (s.nowPlaying.apply _).when(kitchen).returning(Future.successful(Some(CurrentTrack("Piece by Piece", "Slayer"))))
    val parameters = WebhookParameters(Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.NowPlaying, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.nowPlaying.apply _).verify(kitchen)
        response shouldBe Valid(
          WebhookResponse(
            Event.CurrentlyPlaying,
            parameters ++ (Parameter.CurrentArtist -> "Slayer", Parameter.CurrentTitle -> "Piece by Piece")))
    }
  }

  it should "say that nothing is playing in a room that is connected but not currently playing anything" in {
    val s: Services = createServices()
    (s.nowPlaying.apply _).when(kitchen).returning(Future.successful(None))
    val parameters = WebhookParameters(Parameter.Room -> kitchen.name)
    val request = WebhookRequest(Action.NowPlaying, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.nowPlaying.apply _).verify(kitchen)
        response shouldBe Valid(
          WebhookResponse(Event.NothingPlaying, parameters))
    }
  }

  it should "reject a now-playing request for a non-connected room" in {
    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Room -> bedroom.name)
    val request = WebhookRequest(Action.NowPlaying, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        response shouldBe Valid(
          WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "list all the albums for an artist" in {
    val s: Services = createServices()
    val parameters = WebhookParameters(Parameter.Artist -> queen.name)
    val request = WebhookRequest(Action.BrowseArtist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        response shouldBe Valid(
          WebhookResponse(Event.AlbumsForArtist, parameters + (Parameter.Albums -> "A Night at the Opera, Greatest Hits")))
    }
  }

  it should "run an update when asked" in {
    val s: Services = createServices()
    val parameters = WebhookParameters()
    val request = WebhookRequest(Action.Update, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.mediaUpdateMediator.update _).verify()
        response shouldBe Valid(
          WebhookResponse(Event.Updating, parameters))
    }
  }
}

trait Data extends AsyncMockFactory {

  val kitchen: Room = room("Kitchen")
  val lounge: Room = room("Lounge")
  val bedroom: Room = room("Bedroom")
  val connectedRooms: Set[Room] = Set(kitchen, lounge)

  val allRooms = Seq(kitchen, lounge, bedroom)
  val queen: Artist = artist("Queen")
  val metallica: Artist = artist("Metallica")
  val thePolice: Artist = artist("The Police")
  val allArtists = Seq(queen, metallica, thePolice)

  val greatestHits: Album = album("Greatest Hits", queen, thePolice)
  val aNightAtTheOpera: Album = album("A Night at the Opera", queen)
  val deathMagnetic: Album = album("Death Magnetic", metallica)
  val allAlbums = Seq(greatestHits, deathMagnetic, aNightAtTheOpera)

  val planetRock: Favourite = favourite("Planet Rock")
  val allFavourites = Seq(planetRock)

  val mesmerizeAndHypnotize: Playlist = playlist("Mesmerize And Hypnotize")
  val allPlaylists = Seq(mesmerizeAndHypnotize)

  private def emptyEntry = models.Entry("", Seq.empty)
  private def room(name: String) = Room(name, name, emptyEntry)
  private def artist(name: String) = Artist(name, emptyEntry)
  private def album(title: String, artist: Artist, artists: Artist*) = Album(title, Set(artist) ++ artists, emptyEntry)
  private def favourite(favourite: String) = Favourite(favourite, favourite, emptyEntry)
  private def playlist(playlist: String) = Playlist(playlist, playlist, playlist, emptyEntry)

  def createServices(): Services = {
    val musicPlayer: MusicPlayer = stub[MusicPlayer]
    val mediaCache: MediaCacheView = stub[MediaCacheView]

    allRooms.foreach { room =>
      (mediaCache.player _).when(room.name).returning(Some(room))
    }
    (musicPlayer.rooms _).when().returning(Future.successful(connectedRooms))
    allAlbums.foreach { album =>
      (mediaCache.album _).when(album.title).returning(Some(album))
    }
    allFavourites.foreach { favourite =>
      (mediaCache.favourite _).when(favourite.name).returning(Some(favourite))
    }
    allPlaylists.foreach { playlist =>
      (mediaCache.playlist _).when(playlist.name).returning(Some(playlist))
    }
    allArtists.foreach { artist =>
      (mediaCache.artist _).when(artist.name).returning(Some(artist))
    }
    val albumsByArtist: Map[Artist, Set[Album]] =
      allAlbums.foldLeft(Map.empty[Artist, Set[Album]]) { (albumsByArtist, album) =>
        album.artists.foldLeft(albumsByArtist) { (albumsByArtist, artist) =>
          albumsByArtist + (artist -> (albumsByArtist.getOrElse(artist, Set.empty[Album]) + album))
        }
      }
    albumsByArtist.foreach {
      case (artist, albums) =>
        (mediaCache.listAlbums _).when(artist).returning(albums)
    }

    (musicPlayer.playFavourite _).when(*, *).returning(Future.successful({}))
    (musicPlayer.playPlaylist _).when(*, *).returning(Future.successful({}))
    (musicPlayer.playAlbum _).when(*, *, *).returning(Future.successful({}))

    val nowPlaying: NowPlayingService = stub[NowPlayingService]
    val mediaUpdateMediator: MediaUpdateMediator = stub[MediaUpdateMediator]
    (mediaUpdateMediator.update _).when().returning(Future.successful({}))
    val webhookService = new WebhookServiceImpl(musicPlayer, mediaCache, nowPlaying, mediaUpdateMediator)
    Services(musicPlayer, mediaCache, nowPlaying, mediaUpdateMediator, webhookService)
  }

  case class Services(
                       musicPlayer: MusicPlayer,
                       mediaCache: MediaCacheView,
                       nowPlaying: NowPlayingService,
                       mediaUpdateMediator: MediaUpdateMediator,
                       webhookService: WebhookService)
}
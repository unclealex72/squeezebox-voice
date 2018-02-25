package data

import lexical.RemovePunctuationServiceImpl
import media.{MapMediaCache, MediaCacheView, MediaUpdateMediator}
import models._
import org.scalamock.scalatest.AsyncMockFactory
import squeezebox.{MusicPlayer, NowPlayingService}
import webhook.{WebhookService, WebhookServiceImpl}

import scala.concurrent.Future

/**
  * Created by alex on 24/02/18
  **/
trait Data extends AsyncMockFactory {

  val bedroom: Room = room("Bedroom")
  val lounge: Room = room("Lounge")
  val conservatory: Room = room("Conservatory")
  val connectedRooms: Set[Room] = Set(bedroom, conservatory)

  val allRooms = Set(bedroom, lounge, conservatory)
  val queen: Artist = artist("Queen")
  val metallica: Artist = artist("Metallica")
  val thePolice: Artist = artist("The Police")
  val allArtists = Set(queen, metallica, thePolice)

  val greatestHits: Album = album("Greatest Hits", queen, thePolice)
  val aNightAtTheOpera: Album = album("A Night at the Opera", queen)
  val deathMagnetic: Album = album("Death Magnetic", metallica)
  val allAlbums = Set(greatestHits, deathMagnetic, aNightAtTheOpera)

  val planetRock: Favourite = favourite("Planet Rock")
  val allFavourites = Set(planetRock)

  val mesmerizeAndHypnotize: Playlist = playlist("Mezmerize And Hypnotize")
  val allPlaylists = Set(mesmerizeAndHypnotize)

  private def entryOf(entry: String) = models.Entry(entry, Seq.empty)
  private def room(name: String) = Room(name, name, entryOf(name))
  private def artist(name: String) = Artist(name, entryOf(name))
  private def album(title: String, artist: Artist, artists: Artist*) = Album(title, Set(artist) ++ artists, entryOf(title))
  private def favourite(favourite: String) = Favourite(favourite, favourite, entryOf(favourite))
  private def playlist(playlist: String) = Playlist(playlist, playlist, playlist, entryOf(playlist))

  def createServices(): Services = {
    val musicPlayer: MusicPlayer = stub[MusicPlayer]
    val mediaCache: MapMediaCache = new MapMediaCache(new RemovePunctuationServiceImpl)

    mediaCache.updateRooms(allRooms)
    mediaCache.updateAlbums(allAlbums)
    mediaCache.updateFavourites(allFavourites)
    mediaCache.updatePlaylists(allPlaylists)
    mediaCache.updateArtists(allArtists)

    (musicPlayer.connectedRooms _).when().returning(Future.successful(connectedRooms))
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

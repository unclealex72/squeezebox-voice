package webhook

import cats.data.Validated.Valid
import data.Data
import org.scalatest._
import squeezebox.CurrentTrack

import scala.concurrent.Future

/**
  * Created by alex on 11/02/18
  **/
class WebhookServiceImplSpec extends AsyncFlatSpec with Matchers with OneInstancePerTest with Data {

  behavior of "webhook service"

  it should "play a favourite on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withFavourite(planetRock).withRoom(bedroom)
    val request = WebhookRequest(Action.PlayFavourite, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playFavourite _).verify(bedroom, planetRock)
        response shouldBe Valid(WebhookResponse(Event.PlayingFavourite, parameters))
    }
  }

  it should "reject playing a favourite on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withFavourite(planetRock).withRoom(lounge)
    val request = WebhookRequest(Action.PlayFavourite, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "play an album on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withAlbum(deathMagnetic).withRoom(bedroom)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playAlbum _).verify(bedroom, deathMagnetic, metallica)
        response shouldBe Valid(
          WebhookResponse(
            Event.PlayingAlbum,
            parameters.withArtist(metallica)))
    }
  }

  it should "reject playing an album on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withAlbum(deathMagnetic).withRoom(lounge)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "play a playlist item on the squeezebox in a connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withPlaylist(mesmerizeAndHypnotize).withRoom(bedroom)
    val request = WebhookRequest(Action.PlayPlaylist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playPlaylist _).verify(bedroom, mesmerizeAndHypnotize)
        response shouldBe Valid(
          WebhookResponse(Event.PlayingPlaylist, parameters))
    }
  }

  it should "reject playing a playlist item on the squeezebox in a non-connected room" in {

    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withPlaylist(mesmerizeAndHypnotize).withRoom(lounge)
    val request = WebhookRequest(Action.PlayPlaylist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response => response shouldBe Valid(WebhookResponse(Event.RoomNotConnected, parameters))
    }
  }

  it should "query which artist for an album with more than one artist" in {
    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withAlbum(greatestHits).withRoom(bedroom)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        response shouldBe Valid(
          WebhookResponse(
            Event.ArtistRequired,
            parameters.withArtists(queen, thePolice),
            Seq(Context.ArtistRequired)))
    }
  }

  it should "play an album when both an album and and artist are provided" in {
    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().
      withAlbum(greatestHits).
      withArtist(queen).
      withArtists(queen, thePolice).
      withRoom(bedroom)
    val request = WebhookRequest(Action.PlayAlbum, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.musicPlayer.playAlbum _).verify(bedroom, greatestHits, queen)
        response shouldBe Valid(
          WebhookResponse(
            Event.PlayingAlbum,
            parameters))
    }
  }

  it should "say what is playing on a room that is connected and currently playing something" in {
    val s: Services = createServices()
    (s.nowPlaying.apply _).when(bedroom).returning(Future.successful(Some(CurrentTrack("Piece by Piece", "Slayer"))))
    val parameters: WebhookParameters = WebhookParameters().withRoom(bedroom)
    val request = WebhookRequest(Action.NowPlaying, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.nowPlaying.apply _).verify(bedroom)
        response shouldBe Valid(
          WebhookResponse(
            Event.CurrentlyPlaying,
            parameters.withCurrentArtist("Slayer").withCurrentTitle("Piece by Piece")))
    }
  }

  it should "say that nothing is playing in a room that is connected but not currently playing anything" in {
    val s: Services = createServices()
    (s.nowPlaying.apply _).when(bedroom).returning(Future.successful(None))
    val parameters: WebhookParameters = WebhookParameters().withRoom(bedroom)
    val request = WebhookRequest(Action.NowPlaying, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        (s.nowPlaying.apply _).verify(bedroom)
        response shouldBe Valid(
          WebhookResponse(Event.NothingPlaying, parameters))
    }
  }

  it should "reject a now-playing request for a non-connected room" in {
    val s: Services = createServices()
    val parameters: WebhookParameters = WebhookParameters().withRoom(lounge)
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
    val parameters: WebhookParameters = WebhookParameters().withArtist(queen)
    val request = WebhookRequest(Action.BrowseArtist, parameters)
    val eventualResponse = s.webhookService(request)
    eventualResponse.map {
      response =>
        response shouldBe Valid(
          WebhookResponse(Event.AlbumsForArtist, parameters.withAlbums(aNightAtTheOpera, greatestHits)))
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


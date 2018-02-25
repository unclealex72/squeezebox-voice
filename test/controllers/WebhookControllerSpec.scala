package controllers

import java.io.FileNotFoundException

import _root_.matchers.JsonMatchers
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import data.Data
import org.scalatest._
import play.api.http.Writeable
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.{FakeRequest, Helpers}
import squeezebox.CurrentTrack

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by alex on 11/02/18
  **/
class WebhookControllerSpec extends AsyncFlatSpec with Matchers with JsonMatchers with OneInstancePerTest with Data {

  behavior of "webhook service"

  it should "play a favourite on the squeezebox in a connected room" in {
    executeTest(
      "play-favourite-connected",
      expectations = s => (s.musicPlayer.playFavourite _).verify(bedroom, planetRock)
    )
  }

  it should "reject playing a favourite on the squeezebox in a non-connected room" in {
    executeTest("play-favourite-disconnected")
  }

  it should "play an album on the squeezebox in a connected room" in {
    executeTest(
      "play-album-connected",
      expectations = s => (s.musicPlayer.playAlbum _).verify(bedroom, deathMagnetic, metallica))
  }

  it should "reject playing an album on the squeezebox in a non-connected room" in {
    executeTest("play-album-disconnected")
  }

  it should "play a playlist item on the squeezebox in a connected room" in {
    executeTest(
      "play-playlist-connected",
      expectations = s => (s.musicPlayer.playPlaylist _).verify(bedroom, mesmerizeAndHypnotize)
    )
  }

  it should "reject playing a playlist item on the squeezebox in a non-connected room" in {
    executeTest("play-playlist-disconnected")
  }

  it should "query which artist for an album with more than one artist" in {
    executeTest("query-artist-connected")
  }

  it should "play an album when both an album and and artist are provided" in {
    executeTest(
      "play-album-with-artist-connected",
      expectations = s => (s.musicPlayer.playAlbum _).verify(bedroom, greatestHits, queen)
    )
  }

  it should "say what is playing on a room that is connected and currently playing something" in {
    executeTest(
      "current-track-connected",
      mocks = s => (s.nowPlaying.apply _).when(bedroom).returning(Future.successful(Some(CurrentTrack("Bohemian Rhapsody", "Queen")))),
      expectations = s => (s.nowPlaying.apply _).verify(bedroom)
    )
  }

  it should "say that nothing is playing in a room that is connected but not currently playing anything" in {
    executeTest(
      "current-track-empty-connected",
      mocks = s => (s.nowPlaying.apply _).when(bedroom).returning(Future.successful(None)),
      expectations = s => (s.nowPlaying.apply _).verify(bedroom)
    )
  }

  it should "reject a now-playing request for a non-connected room" in {
    executeTest("current-track-disconnected")
  }

  it should "list all the albums for an artist" in {
    executeTest("browse-artist")
  }

  it should "run an update when asked" in {
    executeTest("update")
  }

  def executeTest(
                   name: String,
                   mocks: Services => Unit = _ => {},
                   expectations: Services => Unit = _ => {}): Future[Assertion] = {
    implicit val sys: ActorSystem = ActorSystem("MyTest")
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = Helpers.defaultAwaitTimeout
    implicit val w: Writeable[AnyContentAsJson] = Helpers.writeableOf_AnyContentAsJson
    try {
      val s: Services = createServices()
      mocks(s)
      val controller: WebhookController =
        new WebhookController(
          Helpers.stubControllerComponents(
            playBodyParsers = Helpers.stubPlayBodyParsers(mat)),
          s.webhookService)
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest().withJsonBody(readJson(s"$name.request.json"))
      val eventualResult: Future[Result] = Helpers.call(controller.webhook, request)
      eventualResult.map { result =>
        expectations(s)
        val status: Int = Helpers.status(Future.successful(result))
        val actualJson: JsValue = Helpers.contentAsJson(Future.successful(result))
        val expectedJson: JsValue = readJson(s"$name.response.json")
        status shouldBe Helpers.OK
        Json.asciiStringify(actualJson) should equalsToJson(Json.asciiStringify(expectedJson))
      }
    }
    finally {
      Await.result(sys.terminate(), 1.minute)
    }
  }

  def readJson(resourceName: String): JsValue = {
    Option(getClass.getClassLoader.getResourceAsStream(resourceName)) match {
      case Some(in) => try {
        Json.parse(in)
        }
        finally {
          in.close()
        }
      case None => throw new FileNotFoundException(s"Cannot find resource $resourceName")
    }
  }
}


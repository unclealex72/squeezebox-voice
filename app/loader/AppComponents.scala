package loader

import java.io.FileInputStream

import com.typesafe.scalalogging.StrictLogging
import controllers.{AssetsComponents, UpdateController, WebhookController}
import dialogflow.{UploadEntitiesService, UploadEntitiesServiceImpl}
import hacks.{AlbumTrackReportingReversalHack, SequenceMatchingAlbumTrackReportingReversalHack}
import lexical._
import media._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Configuration}
import play.filters.HttpFiltersComponents
import router.Routes
import squeezebox._
import webhook.{WebhookService, WebhookServiceImpl}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class AppComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with AssetsComponents
    with StrictLogging {

  val dialogFlowToken: String = configuration.get[String]("dialogFlow.token")
  val uploadEntitiesService: UploadEntitiesService =
    new UploadEntitiesServiceImpl(
      ws = wsClient,
      authorisationToken = dialogFlowToken,
      timeout = configuration.get[Duration]("dialogFlow.timeout"))
  val removePunctuationService: RemovePunctuationService = new RemovePunctuationServiceImpl()

  val albumTrackReportingReversalHack: AlbumTrackReportingReversalHack = hacks()
  val roomsProvider: RoomsProvider = rooms(removePunctuationService)

  val squeezeCentreLocation: SqueezeCentreLocation = new ConfiguredSqueezeCentreLocation(configuration = configuration)
  val romanNumeralsService: RomanNumeralsService = new RomanNumeralsServiceImpl()
  val synonymService: SynonymService = new RomanNumeralSynonymService(romanNumeralsService = romanNumeralsService)
  val commandService: CommandService = {
    val ec: ExecutionContext = actorSystem.dispatchers.lookup("squeezeboxCentre-dispatcher")
    new SocketCommandService(squeezeCentreLocation = squeezeCentreLocation)(ec)
  }

  val (musicPlayer, musicRepository): (MusicPlayer, MusicRepository) = both(
    new MusicPlayerImpl(
      commandService = commandService,
      synonymService = synonymService,
      removePunctuationService = removePunctuationService)
  )
  val (mediaCacheView, mediaCacheUpdater): (MediaCacheView, MediaCacheUpdater) = both(
    new MapMediaCache(removePunctuationService = removePunctuationService)
  )
  val mediaUpdateMediator: MediaUpdateMediator =
    new MediaUpdateMediatorImpl(
      mediaCacheUpdater = mediaCacheUpdater,
      musicRepository = musicRepository,
      uploadEntitiesService = uploadEntitiesService,
      roomsProvider = roomsProvider)

  val nowPlayingService: NowPlayingService = new NowPlayingServiceImpl(
    musicPlayer = musicPlayer,
    albumTrackReportingReversalHack = albumTrackReportingReversalHack)

  val webhookService: WebhookService = new WebhookServiceImpl(
    musicPlayer = musicPlayer,
    mediaCacheView = mediaCacheView,
    nowPlayingService = nowPlayingService,
    mediaUpdateMediator = mediaUpdateMediator)

  // Startup

  Await.result(mediaUpdateMediator.update, 1.minute)
  val updateController =
    new UpdateController(
      cc = controllerComponents,
      mediaUpdateMediator = mediaUpdateMediator)
  val webhookController =
    new WebhookController(
      cc = controllerComponents,
      webhookService = webhookService)

  override def router: Router = new Routes(
    httpErrorHandler,
    updateController,
    webhookController
  )

  def both[M1, M2](m: M1 with M2): (M1, M2) = (m, m)

  def hacks(): AlbumTrackReportingReversalHack = {
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
    val hacks = Hacks(configuration)
    new SequenceMatchingAlbumTrackReportingReversalHack(hacks.albumArtistSwaps)
  }

  def rooms(removePunctuationService: RemovePunctuationService): RoomsProvider = {
    val roomsFile: String = configuration.get[String]("rooms.path")
    val in = new FileInputStream(roomsFile)
    val roomNamesById: Map[String, String] = try {
      Json.parse(in).as[Map[String, String]]
    }
    finally {
      in.close()
    }
    new StaticRoomsProvider(roomNamesById, removePunctuationService)

  }
}

package modules

import javax.inject.{Inject, Provider, Singleton}

import akka.actor.ActorSystem
import lexical._
import media._
import play.api.inject._
import squeezebox._

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 26/12/17
  **/

class SqueezeboxModule extends SimpleModule(
  bind[SqueezeCentreLocation].to[ConfiguredSqueezeCentreLocation].eagerly(),
  bind[RomanNumeralsService].to[RomanNumeralsServiceImpl].eagerly(),
  bind[SynonymService].to[RomanNumeralSynonymService].eagerly(),
  bind[MusicPlayer].to[MusicPlayerImpl].eagerly(),
  bind[MusicRepository].to[MusicPlayerImpl].eagerly(),
  bind[MediaCacheView].to[MapMediaCache].eagerly(),
  bind[MediaCacheUpdater].to[MapMediaCache].eagerly(),
  bind[MediaUpdateMediator].to[MediaUpdateMediatorImpl].eagerly(),
  bind[CommandService].toProvider[CommandServiceProvider].eagerly(),
  bind[NowPlayingService].to[NowPlayingServiceImpl].eagerly()) {
}

@Singleton
class CommandServiceProvider @Inject() (actorSystem: ActorSystem, mediaCentre: SqueezeCentreLocation) extends Provider[CommandService] {
  val ec: ExecutionContext = actorSystem.dispatchers.lookup("squeezeboxCentre-dispatcher")
  lazy val get = new SocketCommandService(mediaCentre)(ec)
}

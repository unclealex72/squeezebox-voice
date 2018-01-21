package modules

import javax.inject.{Inject, Provider, Singleton}

import akka.actor.ActorSystem
import play.api.inject.{SimpleModule, _}
import squeezebox._

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 26/12/17
  **/

class SqueezeboxModule extends SimpleModule(
  bind[MediaCentre].to[ConfiguredMediaCentre].eagerly(),
  bind[RomanNumeralsService].to[RomanNumeralsServiceImpl].eagerly(),
  bind[SynonymService].to[RomanNumeralSynonymService].eagerly(),
  bind[SqueezeCentre].to[SqueezeCentreImpl].eagerly(),
  bind[MediaCache].to[MapMediaCache].eagerly(),
  bind[MediaUpdateMediator].to[MediaUpdateMediatorImpl].eagerly(),
  bind[CommandService].toProvider[CommandServiceProvider].eagerly()) {
}

@Singleton
class CommandServiceProvider @Inject() (actorSystem: ActorSystem, mediaCentre: MediaCentre) extends Provider[CommandService] {
  val ec: ExecutionContext = actorSystem.dispatchers.lookup("mediacentre-dispatcher")
  lazy val get = new SocketCommandService(mediaCentre)(ec)
}

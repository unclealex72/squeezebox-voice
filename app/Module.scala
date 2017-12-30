import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import squeezebox._

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 26/12/17
  **/

class Module extends AbstractModule {
  def configure() = {

    bind(classOf[MediaCentre]).to(classOf[ConfiguredMediaCentre]).asEagerSingleton()
    bind(classOf[RomanNumeralsService]).to(classOf[RomanNumeralsServiceImpl]).asEagerSingleton()
    bind(classOf[SynonymService]).to(classOf[RomanNumeralSynonymService]).asEagerSingleton()
    bind(classOf[SqueezeCentre]).to(classOf[SqueezeCentreImpl]).asEagerSingleton()
    bind(classOf[MediaCache]).to(classOf[MapMediaCache]).asEagerSingleton()
    bind(classOf[MediaUpdateMediator]).to(classOf[MediaUpdateMediatorImpl]).asEagerSingleton()
  }

  @Provides
  def provideCommandService(actorSystem: ActorSystem, mediaCentre: MediaCentre): CommandService = {
    val ec: ExecutionContext = actorSystem.dispatchers.lookup("mediacentre-dispatcher")
    new SocketCommandService(mediaCentre)(ec)
  }
}
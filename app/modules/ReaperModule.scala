package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * Created by alex on 01/01/18
  **/
class ReaperModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    //bindActor[ReaperActor]("reaper-actor")
    //bind(classOf[ReapExpiredTokensTask]).asEagerSingleton()
  }
}

package modules

import javax.inject.{Inject, Singleton}

import media.MediaUpdateMediator
import play.api.Logger
import play.api.inject.{SimpleModule, _}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by alex on 26/12/17
  **/

class StartupModule extends SimpleModule(
  bind[Startup].to[StartupImpl].eagerly()) {
}

trait Startup
@Singleton
class StartupImpl @Inject() (mediaUpdateMediator: MediaUpdateMediator)(implicit executionContext: ExecutionContext) extends Startup{

  Await.result(mediaUpdateMediator.update, 1.minute)
}

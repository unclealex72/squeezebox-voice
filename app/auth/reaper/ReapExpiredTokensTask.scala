package auth.reaper

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by alex on 01/01/18
  **/
class ReapExpiredTokensTask @Inject() (actorSystem: ActorSystem, @Named("reaper-actor") reaperActor: ActorRef)(implicit ec: ExecutionContext) {

  actorSystem.scheduler.schedule(
    initialDelay = 0.microseconds,
    interval = 30.seconds,
    receiver = reaperActor,
    message = "reap"
  )
}

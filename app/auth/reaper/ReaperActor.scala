package auth.reaper

import java.time.Clock
import javax.inject.Inject

import akka.actor.Actor
import auth.{AccessTokenTimeoutService, OauthAccessTokenDao}

import scala.concurrent.ExecutionContext
import play.api.Logger

/**
  * Created by alex on 01/01/18
  **/
class ReaperActor @Inject()(clock: Clock, oauthAccessTokenDao: OauthAccessTokenDao, accessTokenTimeoutService: AccessTokenTimeoutService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "reap" =>
      val expireTime = clock.instant().minus(accessTokenTimeoutService.timeout)
      Logger.info("Reaping expired access tokens.")
      oauthAccessTokenDao.deleteCreatedBefore(expireTime).map { expiryCount =>
        Logger.info(s"Reaped $expiryCount access tokens.")
      }
  }
}

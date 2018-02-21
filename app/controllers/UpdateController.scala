package controllers

import javax.inject._

import media.{MediaCacheView, MediaUpdateMediator}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import squeezebox._

import scala.concurrent.ExecutionContext

/**
 * The controller used to send a request to synchronise DialogFlow with the SqueezeCentre.
 */
@Singleton
class UpdateController @Inject()(cc: ControllerComponents, mediaUpdateMediator: MediaUpdateMediator)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def update(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mediaUpdateMediator.update.map(_ => Ok(JsString("Updated"))).recover {
      case e: Exception =>
        Logger.error("An error occurred whilst updating", e)
        InternalServerError(JsString(e.getMessage))
    }
  }

}

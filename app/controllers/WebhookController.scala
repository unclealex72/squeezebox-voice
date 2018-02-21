package controllers

import javax.inject.{Inject, Singleton}

import cats.data.Validated.Invalid
import cats.data._
import cats.implicits._
import media.MediaCacheView
import monads._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, ControllerComponents, Result}
import webhook.{WebhookRequest, WebhookResponse, WebhookService}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 26/01/18
  *
  * The controller to handle requests from and responses to DialogFlow.
  **/
@Singleton
class WebhookController @Inject()(cc: ControllerComponents, webhookService: WebhookService)(implicit ec: ExecutionContext, mediaCache: MediaCacheView) extends AbstractController(cc) {

  /**
    * The entry point for DialogFlow
    * @return A response corresponding to a DialogFlow request.
    */
  def webhook: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    val body = request.body
    Logger.info(Json.prettyPrint(body))
    val evWebhookRequest = Future.successful {
      parseWebhookRequest(body)
    }
    val evWebhookResponse = for {
      webhookRequest <- evWebhookRequest.>
      webhookResponse <- webhookService(webhookRequest).>
    } yield {
      webhookResponse
    }
    evWebhookResponse.fold(respondWithErrors, respondWithSuccess)
  }

  /**
    * Parse a webhook request from a JSON body.
    * @param body the body to parse.
    * @return A webhook request or failure.
    */
  def parseWebhookRequest(body: JsValue): Validated[NonEmptyList[String], WebhookRequest] = {
    body.validate[WebhookRequest] match {
      case JsSuccess(webhookRequest, _) => webhookRequest.validNel
      case JsError(errors) =>
        val errorMessages = errors.flatMap {
          case (path, validationErrors) => validationErrors.map { validationError =>
            s"$path: $validationError"
          }
        }
        Invalid(NonEmptyList.fromListUnsafe(errorMessages.toList))
    }
  }

  /**
    * Send a bad request response back to DialogFlow.
    * @param errors The errors to send.
    * @return A bad request containing the errors.
    */
  def respondWithErrors(errors: NonEmptyList[String]): Result = {
    val errorList = errors.toList
    errorList.foreach(Logger.error(_))
    BadRequest(Json.toJson(errorList))
  }

  /**
    * Send an OK response back to DialogFlow.
    * @param webhookResponse The response to send.
    * @return An OK response.
    */
  def respondWithSuccess(webhookResponse: WebhookResponse): Result = {
    Ok(Json.toJson(webhookResponse))
  }

}

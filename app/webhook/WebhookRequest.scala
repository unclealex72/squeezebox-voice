package webhook

import media.MediaCache

/**
  * Created by alex on 28/01/18
  *
  * A request supplied by DialogFlow
  **/
case class WebhookRequest(action: String, parameters: WebhookParameters)

object WebhookRequest {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import WebhookParameters._

  /**
    * Write a webhook request as JSON.
    */
  implicit def webhookRequestReads(implicit mediaCache: MediaCache): Reads[WebhookRequest] = {
    val result = JsPath \ "result"
    ((result \ "action").read[String] and
      (result \ "parameters").read[WebhookParameters])(WebhookRequest.apply _)
  }

}
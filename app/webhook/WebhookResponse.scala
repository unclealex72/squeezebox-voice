package webhook


/**
  * Created by alex on 28/01/18
  *
  * A response back to DialogFlow that sends an event and contexts.
  **/
case class WebhookResponse(event: String, parameters: WebhookParameters, contextNames: Seq[String] = Seq.empty) {
}

object WebhookResponse {

  import play.api.libs.json._
  import WebhookParameters._

  implicit val writesWebhookResponse: Writes[WebhookResponse] = {
    (webhookResponse: WebhookResponse) => {
      val contexts = webhookResponse.contextNames.map { contextName =>
        Json.obj("name" -> contextName, "parameters" -> webhookResponse.parameters)
      }
      Json.obj(
        "followupEvent" ->
          Json.obj(
            "name" -> webhookResponse.event,
            "data" -> webhookResponse.parameters,
            "contexts" -> contexts))
    }
  }
}
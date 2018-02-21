package webhook


/**
  * Created by alex on 28/01/18
  *
  * A response back to DialogFlow that sends an event and contexts.
  **/
case class WebhookResponse(event: Event, parameters: WebhookParameters, contexts: Seq[Context] = Seq.empty) {
}

object WebhookResponse {

  import play.api.libs.json._
  import WebhookParameters._

  implicit val writesWebhookResponse: Writes[WebhookResponse] = {
    (webhookResponse: WebhookResponse) => {
      val contexts = webhookResponse.contexts.map { context =>
        Json.obj("name" -> context.name, "parameters" -> webhookResponse.parameters)
      }
      Json.obj(
        "followupEvent" ->
          Json.obj(
            "name" -> webhookResponse.event.event,
            "data" -> webhookResponse.parameters,
            "contexts" -> contexts))
    }
  }
}
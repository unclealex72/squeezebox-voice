package webhook

import cats.data.ValidatedNel

import scala.concurrent.Future

/**
  * Created by alex on 28/01/18
  *
  * Run the webhook logic.
  **/
trait WebhookService {

  /**
    * Run the webhook logic.
    * @param webhookRequest The request sent by DialogFlow.
    * @return The response to eventually send back to DialogFlow or a list of errors.
    */
  def apply(webhookRequest: WebhookRequest): Future[ValidatedNel[String, WebhookResponse]]
}

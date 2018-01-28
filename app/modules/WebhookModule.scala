package modules

import play.api.inject.{SimpleModule, _}
import webhook.{WebhookService, WebhookServiceImpl}

/**
  * Created by alex on 26/12/17
  **/

class WebhookModule extends SimpleModule(
  bind[WebhookService].to[WebhookServiceImpl].eagerly()) {
}

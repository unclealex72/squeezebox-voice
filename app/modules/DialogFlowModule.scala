package modules

import javax.inject.{Inject, Provider, Singleton}

import com.google.inject.Provides
import com.typesafe.config.Config
import dialogflow.{UploadEntitiesService, UploadEntitiesServiceImpl}
import lexical.{RemovePunctuationService, RemovePunctuationServiceImpl}
import play.api.inject.{SimpleModule, _}

/**
  * Created by alex on 26/12/17
  **/

class DialogFlowModule extends SimpleModule(
  bind[UploadEntitiesService].to[UploadEntitiesServiceImpl].eagerly(),
  bind[RemovePunctuationService].to[RemovePunctuationServiceImpl].eagerly(),
  bind[String].qualifiedWith[DialogFlowToken].toProvider[DialogFlowTokenProvider]) {

  @Provides @DialogFlowToken
  def providesDialogFlowToken(config: Config): String = {
    config.getString("dialogFlow.token")
  }
}

@Singleton
class DialogFlowTokenProvider @Inject() (config: Config) extends Provider[String] {
  lazy val get: String = config.getString("dialogFlow.token")
}
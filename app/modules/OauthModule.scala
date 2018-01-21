package modules

import java.time.Clock

import auth._
import play.api.inject.{SimpleModule, _}

import scalaoauth2.provider.TokenEndpoint

/**
  * Created by alex on 26/12/17
  **/

class OauthModule extends SimpleModule(
  bind[Clock].toInstance(Clock.systemDefaultZone()),
  bind[RandomStringGenerator].toInstance(new RandomStringGeneratorImpl(40)),
  bind[AccessTokenTimeoutService].to[ConfigurationAccessTokenTimeoutService].eagerly(),
  bind[TokenEndpoint].to[TokenEndpointImpl].eagerly(),
  bind[AuthenticationService].to[AuthenticationServiceImpl].eagerly(),
  bind[UserDao].to[SlickUserDao].eagerly(),
  bind[OauthAccessTokenDao].to[SlickOauthAccessTokenDao].eagerly(),
  bind[OauthClientDao].to[SlickOauthClientDao].eagerly(),
  bind[OauthAuthorizationCodeDao].to[SlickOauthAuthorizationCodeDao].eagerly(),
  bind[UserDataHandler].to[UserDataHandlerImpl].eagerly()) {

}

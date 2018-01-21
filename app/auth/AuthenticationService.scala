package auth

import monads.FutureOption

import scala.concurrent.Future

/**
  * Created by alex on 31/12/17
  **/
trait AuthenticationService {
  def createAccessToken(user: User, oauthClient: OauthClient): Future[OauthAccessToken]

  def createAuthorizationCode(user: User, oauthClient: OauthClient): Future[OauthAuthorizationCode]

  def createUser(username: String, password: String): Future[User]

  def createClient(owner: String, clientId: String, clientSecret: String, maybeScope: Option[String], redirectUri: String): FutureOption[OauthClient]
  
  def refresh(user: User, client: OauthClient): Future[OauthAccessToken]

  def findByCredentials(username: String, password: String): FutureOption[User]

}

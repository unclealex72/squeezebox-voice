package auth

import monads.FutureOption

import scala.concurrent.Future
/**
  * Created by alex on 30/12/17
  **/
trait OauthClientDao {
  def findByClientId(clientId: String): FutureOption[OauthClient]

  def findByCredentials(clientId: String, clientSecret: String): FutureOption[OauthClient]

  def create(user: User, clientId: String, clientSecret: String, maybeScope: Option[String], redirectUri: String): Future[OauthClient]
}
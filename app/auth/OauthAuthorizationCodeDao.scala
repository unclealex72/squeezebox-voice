package auth

import monads.FutureOption

import scala.concurrent.Future

/**
  * Created by alex on 30/12/17
  **/
trait OauthAuthorizationCodeDao {
  def delete(code: String): Future[Unit]

  def findByCode(code: String): FutureOption[OauthAuthorizationCode]

  def create(user: User, oauthClient: OauthClient, code: String): Future[OauthAuthorizationCode]
}
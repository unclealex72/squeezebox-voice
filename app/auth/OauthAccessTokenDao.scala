package auth

import java.time.Instant

import scala.concurrent.Future
import monads._

/**
  * Created by alex on 30/12/17
  **/
trait OauthAccessTokenDao {
  def delete(user: User, oauthClient: OauthClient): Future[Unit]

  def deleteCreatedBefore(instant: Instant): Future[Int]

  def findByAccessToken(token: String): FutureOption[OauthAccessToken]

  def findByRefreshToken(refreshToken: String): FutureOption[OauthAccessToken]

  def create(user: User, oauthClient: OauthClient, accessToken: String, refreshToken: String): Future[OauthAccessToken]

  def findByAuthorized(user: User, clientId: String): FutureOption[OauthAccessToken]

}

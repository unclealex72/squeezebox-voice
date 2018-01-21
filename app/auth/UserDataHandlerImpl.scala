package auth

import javax.inject.Inject

import cats.instances.future._
import dates._
import monads._

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

class UserDataHandlerImpl @Inject()(
                                     oauthClientDao: OauthClientDao,
                                     oauthAccessTokenDao: OauthAccessTokenDao,
                                     oauthAuthorizationCodeDao: OauthAuthorizationCodeDao,
                                     authenticationService: AuthenticationService,
                                     accessTokenTimeoutService: AccessTokenTimeoutService)(implicit ec: ExecutionContext) extends UserDataHandler {

  // common

  override def validateClient(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Boolean] = {
    val validUser: FutureOption[Boolean] = for {
      clientCredential <- maybeCredential.^
      clientSecret <- clientCredential.clientSecret.^
      _ <- oauthClientDao.findByCredentials(clientCredential.clientId, clientSecret)
    } yield {
      val grantType = request.grantType
      Seq("refresh_token", "authorization_code").contains(grantType)
    }
    validUser.getOrElse(false)
  }

  override def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = {
    for {
      clientId <- authInfo.clientId.^
      oauthAccessToken <- oauthAccessTokenDao.findByAuthorized(authInfo.user, clientId)
    } yield {
      toAccessToken(oauthAccessToken)
    }
  }


  override def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = {
    val clientId = requireClient(authInfo.clientId)
    for {
      oauthClient <- requireClient(oauthClientDao.findByClientId(clientId))
      oauthAccessToken <- authenticationService.createAccessToken(authInfo.user, oauthClient)
    } yield {
      toAccessToken(oauthAccessToken)
    }
  }

  private def requireClient[V](mv: Option[V]): V = mv.getOrElse(throw new InvalidClient())
  private def requireClient[V](emv: Future[Option[V]]): Future[V] = emv.map(mv => requireClient(mv))

  private def toAccessToken(accessToken: OauthAccessToken) = {
    AccessToken(
      accessToken.accessToken,
      Some(accessToken.refreshToken),
      accessToken.oauthClient.scope,
      Some(accessTokenTimeoutService.timeout.getSeconds),
      accessToken.createdAt.toDate
    )
  }

  override def findUser(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Option[User]] = {
    request match {
      case request: PasswordRequest =>
        authenticationService.findByCredentials(request.username, request.password)
      case _: ClientCredentialsRequest =>
        for {
          clientCredential <- maybeCredential.^
          clientSecret <- clientCredential.clientSecret.^
          oauthClient <- oauthClientDao.findByCredentials(clientCredential.clientId, clientSecret)
        } yield {
          oauthClient.owner
        }
      case _ => Future.successful(None)
    }
  }

  // Refresh token grant

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = {
    for {
      oauthAccessToken <- oauthAccessTokenDao.findByRefreshToken(refreshToken)
    } yield {
      val client = oauthAccessToken.oauthClient
      AuthInfo(
        user = oauthAccessToken.user,
        clientId = Some(client.clientId),
        scope = None,
        redirectUri = Some(client.redirectUri)
      )
    }
  }

  override def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = {
    for {
      clientId <- requireClient(Future.successful(authInfo.clientId))
      client <- requireClient(oauthClientDao.findByClientId(clientId))
      oauthAccessToken <- authenticationService.refresh(authInfo.user, client)
    } yield {
      toAccessToken(oauthAccessToken)
    }
  }

  // Authorization code grant

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = {
    for {
      oauthAuthorizationCode <- oauthAuthorizationCodeDao.findByCode(code)
    } yield {
      AuthInfo(
        user = oauthAuthorizationCode.user,
        clientId = Some(oauthAuthorizationCode.oauthClient.clientId),
        scope = None,
        redirectUri = Some(oauthAuthorizationCode.oauthClient.redirectUri)
      )
    }
  }

  override def deleteAuthCode(code: String): Future[Unit] = {
    oauthAuthorizationCodeDao.delete(code)
  }

  // Protected resource

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    for {
      oauthAccessToken <- oauthAccessTokenDao.findByAccessToken(token)
    } yield {
      toAccessToken(oauthAccessToken)
    }
  }

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = {
    for {
      oauthAccessToken <- oauthAccessTokenDao.findByAccessToken(accessToken.token)
    } yield {
      AuthInfo(
        user = oauthAccessToken.user,
        clientId = Some(oauthAccessToken.oauthClient.clientId),
        scope = None,
        redirectUri = None
      )
    }
  }
}
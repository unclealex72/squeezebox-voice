package auth

import java.security.{MessageDigest, SecureRandom}
import java.time.Clock

import scala.util.Random
import javax.inject.Inject

import monads._
import cats.instances.future._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 31/12/17
  **/
class AuthenticationServiceImpl @Inject() (
                                            clock: Clock,
                                            userDao: UserDao,
                                            oauthAccessTokenDao: OauthAccessTokenDao,
                                            oauthClientDao: OauthClientDao,
                                            oauthAuthorizationCodeDao: OauthAuthorizationCodeDao,
                                            randomStringGenerator: RandomStringGenerator)(implicit ec: ExecutionContext) extends AuthenticationService {

  override def refresh(user: User, client: OauthClient): Future[OauthAccessToken] = {
    for {
      _ <- removeAccessToken(user, client)
      newAccessToken <- createAccessToken(user, client)
    } yield {
      newAccessToken
    }
  }

  override def findByCredentials(username: String, password: String): FutureOption[User] = {
    val hashedPassword = hash(password)
    userDao.findByCredentials(username, hashedPassword)
  }

  def hash(password: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(password.getBytes)
    md.digest.map(b => "%02x".format(if (b < 0) b + 256 else b)).mkString
  }

  override def createAccessToken(user: User, oauthClient: OauthClient): Future[OauthAccessToken] = {
    val accessToken = randomStringGenerator.generate
    val refreshToken = randomStringGenerator.generate
    oauthAccessTokenDao.create(user, oauthClient, accessToken, refreshToken)
  }

  def removeAccessToken(user: User, oauthClient: OauthClient): Future[Unit] = {
    oauthAccessTokenDao.delete(user, oauthClient)
  }

  override def createUser(username: String, password: String): Future[User] = {
    userDao.create(username, hash(password))
  }

  override def createClient(owner: String, clientId: String, clientSecret: String, maybeScope: Option[String], redirectUri: String): FutureOption[OauthClient] = {
    for {
      user <- userDao.findByUsername(owner)
      client <- oauthClientDao.create(user, clientId, clientSecret, maybeScope, redirectUri).^
    } yield {
      client
    }
  }

  override def createAuthorizationCode(user: User, oauthClient: OauthClient): Future[OauthAuthorizationCode] = {
    val authorizationCode = randomStringGenerator.generate
    oauthAuthorizationCodeDao.create(user, oauthClient, authorizationCode)
  }
}

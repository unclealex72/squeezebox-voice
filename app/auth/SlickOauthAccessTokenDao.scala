package auth

import java.time.{Clock, Instant}
import javax.inject.Inject

import monads._
import cats.instances.future._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 31/12/17
  **/
class SlickOauthAccessTokenDao @Inject()(dbConfigProvider: DatabaseConfigProvider, clock: Clock)(implicit ec: ExecutionContext) extends Slick(dbConfigProvider, clock) with OauthAccessTokenDao {

  import dbConfig.profile.api._

  def find(
            tokenFilter: Option[OauthAccessTokens => Rep[Boolean]] = None,
            userFilter: Option[Users => Rep[Boolean]] = None,
            clientFilter: Option[OauthClients => Rep[Boolean]] = None): FutureOption[OauthAccessToken] = {
    val result = dbConfig.db.run {
      val tf: OauthAccessTokens => Rep[Boolean] = tokenFilter.getOrElse(_ => true)
      val uf: Users => Rep[Boolean] = userFilter.getOrElse(_ => true)
      val cf: OauthClients => Rep[Boolean] = clientFilter.getOrElse(_ => true)
      val query = for {
        at <- accessTokens if tf(at)
        u <- users if at.userId === u.id && uf(u)
        cl <- clients if at.oauthClientId === cl.id && cf(cl)
        o <- users if cl.ownerId === o.id
      } yield {
        (at, u, cl, o)
      }
      query.result.headOption
    }
    result.^.map {
      case (at, u, cl, o) =>
        val client = cl.asOauthClient(o)
        at.asOauthAccessToken(u, client)
    }
  }

  override def findByAccessToken(token: String): FutureOption[OauthAccessToken] = {
    find(tokenFilter = Some(_.accessToken === token))
  }

  override def findByRefreshToken(refreshToken: String): FutureOption[OauthAccessToken] = {
    find(tokenFilter = Some(_.refreshToken === refreshToken))
  }

  override def create(user: User, oauthClient: OauthClient, accessToken: String, refreshToken: String): Future[OauthAccessToken] = {
    val createdAt = now
    dbConfig.db.run {
      val table = accessTokens.map(at => (at.userId, at.oauthClientId, at.accessToken, at.refreshToken, at.createdAt))
      val pk = table returning accessTokens.map(_.id) +=
        (user.id, oauthClient.id, accessToken, refreshToken, createdAt)
      pk
    }.map { id =>
      OauthAccessToken(id, user, oauthClient, accessToken, refreshToken, createdAt)
    }
  }

  override def findByAuthorized(user: User, clientId: String): FutureOption[OauthAccessToken] = {
    find(userFilter = Some(_.id === user.id), clientFilter = Some(_.clientId === clientId))
  }

  override def delete(user: User, oauthClient: OauthClient): Future[Unit] = {
    dbConfig.db.run {
      accessTokens.filter(at => at.userId === user.id && at.oauthClientId === oauthClient.id).delete
    }.map(_ => {})
  }

  override def deleteCreatedBefore(instant: Instant): Future[Int] = {
    dbConfig.db.run {
      accessTokens.filter(_.createdAt <= instant).delete
    }
  }
}

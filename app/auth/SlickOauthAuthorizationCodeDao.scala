package auth

import java.time.Clock
import javax.inject.Inject

import cats.instances.future._
import monads._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 31/12/17
  **/
class SlickOauthAuthorizationCodeDao @Inject()(dbConfigProvider: DatabaseConfigProvider, clock: Clock)(implicit ec: ExecutionContext) extends Slick(dbConfigProvider, clock) with OauthAuthorizationCodeDao {

  import dbConfig.profile.api._

  def find(
            authorizationCodeFilter: Option[OauthAuthorizationCodes => Rep[Boolean]] = None,
            userFilter: Option[Users => Rep[Boolean]] = None,
            clientFilter: Option[OauthClients => Rep[Boolean]] = None): FutureOption[OauthAuthorizationCode] = {
    val result = dbConfig.db.run {
      val af: OauthAuthorizationCodes => Rep[Boolean] = authorizationCodeFilter.getOrElse(_ => true)
      val uf: Users => Rep[Boolean] = userFilter.getOrElse(_ => true)
      val cf: OauthClients => Rep[Boolean] = clientFilter.getOrElse(_ => true)
      val query = for {
        ac <- authorizationCodes if af(ac)
        u <- users if ac.userId === u.id && uf(u)
        cl <- clients if ac.clientId === cl.id && cf(cl)
      } yield {
        (ac, u, cl)
      }
      query.result.headOption
    }
    result.^.map {
      case (ac, u, cl) =>
        val client = cl.asOauthClient(u)
        ac.asOauthAuthorizationCode(u, client)
    }
  }

  override def create(user: User, oauthClient: OauthClient, code: String): Future[OauthAuthorizationCode] = {
    val createdAt = now
    dbConfig.db.run {
      val table = authorizationCodes.map(ac => (ac.userId, ac.clientId, ac.code, ac.createdAt))
      val pk = table returning authorizationCodes.map(_.id) +=
        (user.id, oauthClient.id, code, createdAt)
      pk
    }.map { id =>
      OauthAuthorizationCode(id, user, oauthClient, code, createdAt)
    }
  }

  override def delete(code: String): Future[Unit] = {
    dbConfig.db.run {
      authorizationCodes.filter(_.code === code).delete
    }.map(_ => {})

  }

  override def findByCode(code: String): FutureOption[OauthAuthorizationCode] = {
    find(authorizationCodeFilter = Some(_.code === code))
  }
}

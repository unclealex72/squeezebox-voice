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
class SlickOauthClientDao @Inject()(dbConfigProvider: DatabaseConfigProvider, clock: Clock)(implicit ec: ExecutionContext) extends Slick(dbConfigProvider, clock) with OauthClientDao {

  import dbConfig.profile.api._

  def find(
            clientFilter: Option[OauthClients => Rep[Boolean]] = None,
            userFilter: Option[Users => Rep[Boolean]] = None): FutureOption[OauthClient] = {
    val result = dbConfig.db.run {
      val cf: OauthClients => Rep[Boolean] = clientFilter.getOrElse(_ => true)
      val uf: Users => Rep[Boolean] = userFilter.getOrElse(_ => true)
      val query = for {
        cl <- clients if cf(cl)
        u <- users if cl.ownerId === u.id && uf(u)
      } yield {
        (cl, u)
      }
      query.result.headOption
    }
    result.^.map {
      case (cl, u) =>
        cl.asOauthClient(u)
    }
  }

  override def findByClientId(clientId: String): FutureOption[OauthClient] = {
    find(clientFilter = Some(_.clientId === clientId))
  }

  override def findByCredentials(clientId: String, clientSecret: String): FutureOption[OauthClient] = {
    find(clientFilter = Some(cl => cl.clientId === clientId && cl.clientSecret === clientSecret))
  }

  override def create(user: User, clientId: String, clientSecret: String, scope: Option[String], redirectUri: String): Future[OauthClient] = {
    val createdAt = now
    dbConfig.db.run {
      val table = clients.map(c => (c.ownerId, c.clientId, c.clientSecret, c.scope, c.redirectUri, c.createdAt))
      val pk = table returning clients.map(_.id) +=
        (user.id, clientId, clientSecret, scope, redirectUri, createdAt)
      pk
    }.map { id =>
      OauthClient(id, user, clientId, clientSecret, scope, redirectUri, createdAt)
    }
  }

}

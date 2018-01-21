package auth

import java.time.Clock
import javax.inject.Inject

import monads._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 31/12/17
  **/
class SlickUserDao @Inject() (dbConfigProvider: DatabaseConfigProvider, clock: Clock)(implicit ec: ExecutionContext) extends Slick(dbConfigProvider, clock) with UserDao {

  import dbConfig.profile.api._

  override def create(username: String, hashedPassword: String): Future[User] = {
    val createdAt = now
    dbConfig.db.run {
      val table = users.map(u => (u.username, u.hashedPassword, u.createdAt))
      val pk = table returning users.map(_.id) +=
        (username, hashedPassword, createdAt)
      pk
    }.map { id =>
      User(id, username, hashedPassword, createdAt)
    }
  }

  override def findByCredentials(username: String, hashedPassword: String): FutureOption[User] = {
    dbConfig.db.run {
      users.filter(u => u.username === username && u.hashedPassword === hashedPassword).result.headOption
    }.^
  }

  override def findByUsername(username: String): FutureOption[User] = {
    dbConfig.db.run {
      users.filter(u => u.username === username).result.headOption
    }.^
  }
}

package auth

import monads.FutureOption

import scala.concurrent.Future

/**
  * Created by alex on 31/12/17
  **/
trait UserDao {
  def findByCredentials(username: String, hashedPassword: String): FutureOption[User]

  def findByUsername(username: String): FutureOption[User]

  def create(username: String, hashedPassword: String): Future[User]
}

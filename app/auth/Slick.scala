package auth

import java.sql.Timestamp
import java.time.{Clock, Instant}

import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

/**
  * Created by alex on 31/12/17
  **/
class Slick(dbConfigProvider: DatabaseConfigProvider, clock: Clock) {

  def now: Instant = clock.instant()

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._

  implicit val instantColumnType: BaseColumnType[Instant] = MappedColumnType.base[Instant, Timestamp](
    i => new Timestamp(i.toEpochMilli),
    ts => Instant.ofEpochMilli(ts.getTime)
  )

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("USER_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def username = column[String]("USERNAME", O.Unique)
    def hashedPassword = column[String]("HASHED_PASSWORD")
    def createdAt = column[Instant]("CREATED_AT")
    def * = (id, username, hashedPassword, createdAt) <> (User.tupled, User.unapply)
  }
  val users = TableQuery[Users]

  case class OauthClientsTable(
                                id: Int,
                                ownerId: Int,
                                clientId: String,
                                clientSecret: String,
                                scope: Option[String],
                                redirectUri: String,
                                createdAt: Instant) {
    def asOauthClient(user: User): OauthClient =
      OauthClient(id, user, clientId, clientSecret, scope, redirectUri, createdAt)
  }
  class OauthClients(tag: Tag) extends Table[OauthClientsTable](tag, "CLIENTS") {
    def id = column[Int]("CLIENT_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def ownerId = column[Int]("OWNER_ID")
    def clientId = column[String]("CLIENT_PUBLIC_ID", O.Unique)
    def clientSecret = column[String]("CLIENT_SECRET")
    def scope = column[Option[String]]("scope")
    def redirectUri = column[String]("REDIRECT_URI")
    def createdAt = column[Instant]("CREATED_AT")

    def owner = foreignKey("CLIENT_OWNER_FK", ownerId, users)(_.id)

    def * = (id, ownerId, clientId, clientSecret, scope, redirectUri, createdAt) <> (OauthClientsTable.tupled, OauthClientsTable.unapply)
  }
  val clients = TableQuery[OauthClients]

  case class OauthAccessTokensTable(id: Int, userId: Int, oauthClientId: Int, accessToken: String, refreshToken: String, createdAt: Instant) {
    def asOauthAccessToken(user: User, client: OauthClient): OauthAccessToken = {
      OauthAccessToken(id, user, client, accessToken, refreshToken, createdAt)
    }
  }
  class OauthAccessTokens(tag: Tag) extends Table[OauthAccessTokensTable](tag, "ACCESS_TOKENS") {
    def id = column[Int]("ACCESS_TOKEN_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def userId = column[Int]("USER_ID")
    def oauthClientId = column[Int]("CLIENT_ID")
    def accessToken = column[String]("ACCESS_TOKEN")
    def refreshToken = column[String]("REFRESH_TOKEN")
    def createdAt = column[Instant]("CREATED_AT")

    def user = foreignKey("ACCESS_TOKEN_USER_FK", userId, users)(_.id)
    def oauthClient = foreignKey("ACCESS_TOKEN_CLIENT_FK", oauthClientId, clients)(_.id)

    def * = (id, userId, oauthClientId, accessToken, refreshToken, createdAt) <> (OauthAccessTokensTable.tupled, OauthAccessTokensTable.unapply)
  }
  val accessTokens = TableQuery[OauthAccessTokens]

  case class OauthAuthorizationCodesTable(id: Int, userId: Int, clientId: Int, code: String, createdAt: Instant) {
    def asOauthAuthorizationCode(user: User, client: OauthClient): OauthAuthorizationCode = {
      OauthAuthorizationCode(id, user, client, code, createdAt)
    }
  }
  class OauthAuthorizationCodes(tag: Tag) extends Table[OauthAuthorizationCodesTable](tag, "CODES") {
    def id = column[Int]("AUTHORIZATION_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def userId = column[Int]("USER_ID")
    def clientId = column[Int]("CLIENT_ID")
    def code = column[String]("CODE")
    def createdAt = column[Instant]("CREATED_AT")

    def user = foreignKey("CODE_USER_FK", userId, users)(_.id)
    def client = foreignKey("CODE_CLIENT_FK", clientId, clients)(_.id)

    def * = (id, userId, clientId, code, createdAt) <> (OauthAuthorizationCodesTable.tupled, OauthAuthorizationCodesTable.unapply)
  }
  val authorizationCodes = TableQuery[OauthAuthorizationCodes]

  private val allTables = Seq(users, clients, accessTokens, authorizationCodes)
  val createStatements: Seq[String] = allTables.flatMap(_.schema.createStatements)
  val dropStatements: Seq[String] = allTables.reverse.flatMap(_.schema.dropStatements)
}

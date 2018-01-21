package auth

import java.time.Instant

/**
  * Created by alex on 30/12/17
  **/
case class OauthClient(
                        id: Int,
                        owner: User,
                        clientId: String,
                        clientSecret: String,
                        scope: Option[String],
                        redirectUri: String,
                        createdAt: Instant
                      )
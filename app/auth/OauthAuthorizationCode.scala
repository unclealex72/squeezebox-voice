package auth

import java.time.Instant

/**
  * Created by alex on 31/12/17
  **/

case class OauthAuthorizationCode(
                                   id: Int,
                                   user: User,
                                   oauthClient: OauthClient,
                                   code: String,
                                   createdAt: Instant)
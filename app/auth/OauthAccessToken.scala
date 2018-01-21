package auth

import java.time.Instant

/**
  * Created by alex on 30/12/17
  **/
case class OauthAccessToken(
                             id: Int,
                             user: User,
                             oauthClient: OauthClient,
                             accessToken: String,
                             refreshToken: String,
                             createdAt: Instant
                           )
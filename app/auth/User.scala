package auth

import java.time.Instant

/**
  * Created by alex on 30/12/17
  **/
case class User(id: Int, username: String, hashedPassword: String, createdAt: Instant)
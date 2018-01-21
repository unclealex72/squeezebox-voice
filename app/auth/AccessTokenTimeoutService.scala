package auth

import java.time.Duration

/**
  * Created by alex on 01/01/18
  **/
trait AccessTokenTimeoutService {

  def timeout: Duration
}

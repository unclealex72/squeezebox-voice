package auth

import java.time.Duration
import javax.inject.Inject

import play.api.Configuration

/**
  * Created by alex on 01/01/18
  **/
class ConfigurationAccessTokenTimeoutService @Inject() (configuration: Configuration) extends AccessTokenTimeoutService {

  private val _timeout: Duration =
    Duration.ofMillis(configuration.getMillis("tokens.timeout"))

  override def timeout: Duration = _timeout
}

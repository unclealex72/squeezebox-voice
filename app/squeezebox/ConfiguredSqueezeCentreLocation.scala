package squeezebox

import play.api.Configuration

/**
  * A class that gets the [[SqueezeCentreLocation]] from configuration.
  * Created by alex on 26/12/17
  **/
class ConfiguredSqueezeCentreLocation(configuration: Configuration) extends SqueezeCentreLocation {

  val (host: String, port: Int) = {
    (configuration.get[String]("squeezeboxCentre.host"), configuration.get[Int]("squeezeboxCentre.port"))
  }
}

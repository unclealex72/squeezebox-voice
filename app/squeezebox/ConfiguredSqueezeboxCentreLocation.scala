package squeezebox

import javax.inject.Inject

import play.api.Configuration

/**
  * A class that gets the [[SqueezeboxCentreLocation]] from configuration.
  * Created by alex on 26/12/17
  **/
class ConfiguredSqueezeboxCentreLocation @Inject()(configuration: Configuration) extends SqueezeboxCentreLocation {

  val (host: String, port: Int) = {
    (configuration.get[String]("squeezeboxCentre.host"), configuration.get[Int]("squeezeboxCentre.port"))
  }
}

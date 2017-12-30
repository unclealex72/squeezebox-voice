package squeezebox

import javax.inject.Inject

import play.api.Configuration

/**
  * A class that gets the `MediaCentre` information from configuration.
  * Created by alex on 26/12/17
  **/
class ConfiguredMediaCentre @Inject() (configuration: Configuration) extends MediaCentre {

  val (host: String, port: Int) = {
    (configuration.get[String]("mediacentre.host"), configuration.get[Int]("mediacentre.port"))
  }
}

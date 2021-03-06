package squeezebox

/**
  * Created by alex on 23/12/17
  **/
trait SqueezeCentreLocation {

  /**
    * @return The SqueezeboxCentre's host.
    */
  def host: String
  /**
    * @return The SqueezeboxCentre's port.
    */
  def port: Int
}

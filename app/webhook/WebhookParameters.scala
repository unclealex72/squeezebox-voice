package webhook

import cats.data.ValidatedNel
import cats.implicits._
import media.MediaCacheView
import models._

/**
  * Created by alex on 28/01/18
  *
  * A wrapper around a map of strings that contain parameters used by dialogFow.
  **/
case class WebhookParameters(parameters: Map[Parameter, String]) {

  private def optional[A](key: Parameter, finder: String => Option[A]): ValidatedNel[String, Option[A]] = {
    parameters.get(key).filterNot(_.isEmpty) match {
      case Some(theKey) => finder(theKey).toRight(s"${key.name} is not valid").toValidatedNel.map(Some(_))
      case None => None.validNel
    }
  }

  private def mandatory[A](key: Parameter, finder: String => Option[A]): ValidatedNel[String, A] = {
    optional(key, finder) andThen {
      case Some(value) => value.validNel
      case None => s"${key.name} is required".invalidNel
    }
  }

  /**
    * Add a parameter
    * @param kv The key value pair to add.
    * @return A new webhook parameters object with the extra parameter.
    */
  def +(kv: (Parameter, String)): WebhookParameters = ++(kv)

  /**
    * Add a list of parameters.
    * @param kv The first key value pair to add.
    * @param kvs The next key value pairs to add.
    * @return A new webhook parameters object with the extra parameters.
    */
  def ++(kv: (Parameter, String), kvs: (Parameter, String)*): WebhookParameters = {
    WebhookParameters(parameters ++ (kv +: kvs))
  }

  /**
    * Get the room stored in these parameters.
    * @return The room or invalid if it cannot be found.
    */
  def room(mediaCache: MediaCacheView): ValidatedNel[String, Room] =
    mandatory(Parameter.Room, mediaCache.player)

  /**
    * Get the favourite stored in these parameters.
    * @return The favourite or invalid if it cannot be found.
    */
  def favourite(mediaCache: MediaCacheView): ValidatedNel[String, Favourite] =
    mandatory(Parameter.Favourite, mediaCache.favourite)

  /**
    * Get the playlist stored in these parameters.
    * @return The playlist or invalid if it cannot be found.
    */
  def playlist(mediaCache: MediaCacheView): ValidatedNel[String, Playlist] =
    mandatory(Parameter.Playlist, mediaCache.playlist)

  /**
    * Get the album stored in these parameters.
    * @return The album or invalid if it cannot be found.
    */
  def album(mediaCache: MediaCacheView): ValidatedNel[String, Album] =
    mandatory(Parameter.Album, mediaCache.album)

  /**
    * Get the artist stored in these parameters.
    * @return The artist or invalid if it cannot be found.
    */
  def artist(mediaCache: MediaCacheView): ValidatedNel[String, Artist] =
    mandatory(Parameter.Artist, mediaCache.artist)

  /**
    * Get the artist stored in these parameters.
    * @return If the parameter exists return the artist or invalid if it cannot be found. Otherwise return None.
    */
  def maybeArtist(mediaCache: MediaCacheView): ValidatedNel[String, Option[Artist]] =
    optional(Parameter.Artist, mediaCache.artist)

}

object WebhookParameters {

  import play.api.libs.json._

  def apply(parameters: (Parameter, String)*): WebhookParameters = WebhookParameters(parameters.toMap)

  /**
    * Read parameters from JSON.
    * @param mapReads A reader for reading maps.
    * @return A reader for reading parameters from JSON.
    */
  implicit def webhookParametersReads(
                                       implicit mapReads: Reads[Map[String, String]]): Reads[WebhookParameters] = {
    mapReads.map { parameters =>
      val parameterSeq: Map[Parameter, String] = for {
        keyValue <- parameters
        parameter <- Parameter.values.find(_.name == keyValue._1)
      } yield {
        parameter -> keyValue._2
      }
      WebhookParameters(parameterSeq)
    }
  }

  /**
    * Write parameters to JSON.
    * @param mapWrites A writer for writing maps.
    * @return A writer for writing parameters to JSON.
    */
  implicit def webhookParametersWrites(implicit mapWrites: Writes[Map[String, String]], parameterWrites: Writes[Parameter]): Writes[WebhookParameters] = {
    (webhookParameters: WebhookParameters) => mapWrites.writes(webhookParameters.parameters.map(kv => kv._1.name -> kv._2))
  }
}
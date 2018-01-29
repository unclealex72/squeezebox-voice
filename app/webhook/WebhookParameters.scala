package webhook

import cats.data._
import cats.implicits._
import media.MediaCache
import models.{Album, Artist, Favourite, Room}

/**
  * Created by alex on 28/01/18
  *
  * A wrapper around a map of strings that contain parameters used by dialogFow.
  **/
case class WebhookParameters(parameters: Map[String, String]) {

  private def optional[A](key: String, finder: String => Option[A]): ValidatedNel[String, Option[A]] = {
    parameters.get(key).filterNot(_.isEmpty) match {
      case Some(theKey) => finder(theKey).toRight(s"$theKey is not valid").toValidatedNel.map(Some(_))
      case None => None.validNel
    }
  }

  private def mandatory[A](key: String, finder: String => Option[A]): ValidatedNel[String, A] = {
    optional(key, finder) andThen {
      case Some(value) => value.validNel
      case None => s"$key is required".invalidNel
    }
  }

  /**
    * Add a parameter
    * @param kv The key value pair to add.
    * @return
    */
  def +(kv: (String, String)): WebhookParameters = WebhookParameters(parameters + kv)

  /**
    * Get the room stored in these parameters.
    * @return The room or invalid if it cannot be found.
    */
  def room(mediaCache: MediaCache): ValidatedNel[String, Room] = mandatory("room", mediaCache.player)

  /**
    * Get the favourite stored in these parameters.
    * @return The favourite or invalid if it cannot be found.
    */
  def favourite(mediaCache: MediaCache): ValidatedNel[String, Favourite] = mandatory("favourite", mediaCache.favourite)

  /**
    * Get the album stored in these parameters.
    * @return The album or invalid if it cannot be found.
    */
  def album(mediaCache: MediaCache): ValidatedNel[String, Album] = mandatory("album", mediaCache.album)

  /**
    * Get the artist stored in these parameters.
    * @return If the parameter exists return the artist or invalid if it cannot be found. Otherwise return None.
    */
  def maybeArtist(mediaCache: MediaCache): ValidatedNel[String, Option[Artist]] = optional("artist", mediaCache.artist)

}

object WebhookParameters {

  import play.api.libs.json._

  /**
    * Read parameters from JSON.
    * @param mapReads A reader for reading maps.
    * @param mediaCache A media cached used to search for entities.
    * @return A reader for reading parameters from JSON.
    */
  implicit def webhookParametersReads(implicit mapReads: Reads[Map[String, String]], mediaCache: MediaCache): Reads[WebhookParameters] = {
    mapReads.map(WebhookParameters(_))
  }

  /**
    * Write parameters to JSON.
    * @param mapWrites A writer for writing maps.
    * @return A writer for writing parameters to JSON.
    */
  implicit def webhookParametersWrites(implicit mapWrites: Writes[Map[String, String]]): Writes[WebhookParameters] = {
    (webhookParameters: WebhookParameters) => mapWrites.writes(webhookParameters.parameters)
  }
}
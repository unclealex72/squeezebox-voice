package dialogflow
import java.io.IOException
import javax.inject.Inject

import models._
import modules.DialogFlowToken
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 21/01/18
  *
  * The default implementation of the [[UploadEntitiesService]]
  **/
class UploadEntitiesServiceImpl @Inject() (ws: WSClient, @DialogFlowToken authorisationToken: String)(implicit ec: ExecutionContext) extends UploadEntitiesService {

  override def uploadAlbums(albums: Seq[Album]): Future[Unit] = {
    uploadEntity[Album]("album", albums, _.entry)
  }

  override def uploadArtists(artists: Seq[Artist]): Future[Unit] = {
    uploadEntity[Artist]("artist", artists, _.entry)
  }

  override def uploadFavourites(favourites: Seq[Favourite]): Future[Unit] = {
    uploadEntity[Favourite]("favourite", favourites, _.entry)
  }

  override def uploadRooms(rooms: Seq[Room]): Future[Unit] = {
    uploadEntity[Room]("room", rooms, _.entry)
  }

  /**
    * Upload an entity to DialogFlow. An entity is uploaded by first listing all it's existing entries, deleting them
    * and then uploading the new entries.
    * @param name The name of the entry to upload.
    * @param values The list of values to upload.
    * @param builder A function that turns a value into an [[Entry]].
    * @tparam A The type of `values`
    * @return
    */
  def uploadEntity[A](name: String, values: Seq[A], builder: A => Entry): Future[Unit] = {
    def alterEntity[J <: JsValue](method: String, isEntries: Boolean, maybeBody: Option[JsValue] = None)(implicit reads: Reads[J]): Future[J] = {
      val url = (Seq(
        "https://api.dialogflow.com/v1/entities", name
      ) ++ Some("entries").filter(_ => isEntries)).mkString("/")
      val bodilessRequest = ws.url(url).
        addQueryStringParameters("v" -> "20150910").
        addHttpHeaders(
          "Authorization" -> s"Bearer $authorisationToken",
          "Content-Type" -> "application/json").
        withMethod(method)
      val request = maybeBody.foldLeft(bodilessRequest)(_.withBody(_))
      request.execute().map { response =>
        if (response.status == 200) {
          response.body[JsValue].as[J]
        } else {
          throw new IOException((response.body[JsValue] \ "status" \ "errorDetails").asOpt[String].getOrElse("Unknown error"))
        }
      }
    }
    def listEntries(): Future[Seq[String]] =
      alterEntity[JsObject]("GET", isEntries = false).map { body =>
      (body \ "entries").as[JsArray].value.map { obj => (obj \ "value").as[JsString].value }
    }
    def deleteEntries(entries: Seq[String]): Future[JsValue] =
      alterEntity[JsValue]("DELETE", isEntries = true, Some(JsArray(entries.map(JsString))))
    def addEntries(): Future[JsValue] =
      alterEntity[JsValue]("PUT", isEntries = true, Some(JsArray(values.map(builder).map { entry  =>
        val synonyms = entry.synonyms :+ entry.unpunctuated
        Json.obj("value" -> JsString(entry.unpunctuated), "synonyms" -> JsArray(synonyms.map(JsString)))
      })))
    for {
      entries <- listEntries()
      _ <- deleteEntries(entries)
      _ <- addEntries()
    } yield {}
  }


}
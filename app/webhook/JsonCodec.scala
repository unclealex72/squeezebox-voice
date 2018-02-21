package webhook

import play.api.libs.json._

import scala.collection.immutable.IndexedSeq

/**
  * Created by alex on 11/02/18
  **/
trait JsonCodec[A] {

  def tokenFactory: A => String
  val name: String
  val values: IndexedSeq[A]

  implicit def writes: Writes[A] = (o: A) => JsString(tokenFactory(o))

  implicit def optionalReads(implicit stringReads: Reads[String]): Reads[Option[A]] = {
    stringReads.map { token =>
      values.find(value => tokenFactory(value) == token)
    }
  }

  implicit def reads(implicit stringReads: Reads[String]): Reads[A] = Reads[A] { json =>
    stringReads.reads(json).flatMap { token =>
      values.find(value => tokenFactory(value) == token) match {
        case Some(value) => JsSuccess(value)
        case None => JsError(s"$token is not a valid $name")
      }
    }
  }


}


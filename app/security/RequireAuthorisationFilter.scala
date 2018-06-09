package security

import play.api.http.{HttpErrorHandler, Status}
import play.api.libs.streams.Accumulator
import play.api.mvc._

/**
  * Created by alex on 29/01/18
  *
  * A filter that requires an Authorization header to be sent with a value defined within configuration.
  **/
class RequireAuthorisationFilter(token: String, errorHandler: HttpErrorHandler) extends EssentialFilter {

  val header: String = "Authorization"

  val bearer: String = s"Bearer $token"

  override def apply(next: EssentialAction): EssentialAction = { req =>
    val result: Either[(Int, String), Unit] = for {
      auth <- req.headers.get(header).toRight(Status.UNAUTHORIZED -> "Unauthorized")
      _ <- if (auth == bearer) Right({}) else Left(Status.FORBIDDEN -> "forbidden")
    } yield {}
    result match {
      case Right(_) => next(req)
      case Left((status, message)) =>
        Accumulator.done(errorHandler.onClientError(req, status, message))
    }
  }
}

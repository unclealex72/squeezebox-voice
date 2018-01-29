package security

import javax.inject.Inject

import play.api.Configuration
import play.api.http.{HttpErrorHandler, Status}
import play.api.libs.streams.Accumulator
import play.api.mvc._

/**
  * Created by alex on 29/01/18
  *
  * A filter that requires an Authorization header to be sent with a value defined within configuration.
  **/
class RequireAuthorisationFilter @Inject() (config: Configuration, errorHandler: HttpErrorHandler) extends EssentialFilter {

  val header: String = "Authorization"

  val token: String = config.get[String]("security.token")
  val bearer: String = s"Bearer $token"

  override def apply(next: EssentialAction): EssentialAction = { req =>
    val result: Either[(Int, String), Unit] = for {
      auth <- req.headers.get(header).toRight(Status.FORBIDDEN -> "forbidden")
      _ <- if (auth == bearer) Right({}) else Left(Status.UNAUTHORIZED -> "Unauthorized")
    } yield {}
    result match {
      case Right(_) => next(req)
      case Left((status, message)) =>
        Accumulator.done(errorHandler.onClientError(req, status, message))
    }
  }
}

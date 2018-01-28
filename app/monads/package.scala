import cats.data.{EitherT, NonEmptyList, ValidatedNel}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 30/12/17
  *
  * Combined monads.
  **/
package object monads {

  /**
    * The combined monad for `Future` and `Either`
    * @tparam L The left hand side type.
    * @tparam R The right hand side type.
    */
  type FutureEither[L, R] = EitherT[Future, L, R]

  implicit class FeLiftFutureEither[L, R](futureEither: Future[Either[L, R]])(implicit ec: ExecutionContext) {
    def > : FutureEither[L, R] = EitherT[Future, L, R](futureEither)
  }
  implicit class FeLiftEither[L, R](either: Either[L, R])(implicit ec: ExecutionContext) {
    def > : FutureEither[L, R] = Future.successful(either).>
  }
  implicit class FeLiftFuture[L, R](future: Future[R])(implicit ec: ExecutionContext) {
    def > : FutureEither[L, R] = future.map(Right(_)).>
  }
  implicit class FutureValidateNelLift[L, R](fv: Future[ValidatedNel[L, R]])(implicit ec: ExecutionContext) {
    def > : FutureEither[NonEmptyList[L], R] = fv.map(_.toEither).>
  }

  implicit def toValue[L, R](me: FutureEither[L, R]): Future[Either[L, R]] = me.value
}

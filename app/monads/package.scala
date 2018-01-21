import cats.data.OptionT

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 30/12/17
  **/
package object monads {

  type FutureOption[A] = OptionT[Future, A]

  implicit class LiftFutureOption[A](futureOption: Future[Option[A]])(implicit ec: ExecutionContext) {
    def ^ : FutureOption[A] = OptionT[Future, A](futureOption)
  }
  implicit class LiftOption[A](option: Option[A])(implicit ec: ExecutionContext) {
    def ^ : FutureOption[A] = Future.successful(option).^
  }
  implicit class LiftFuture[A](future: Future[A])(implicit ec: ExecutionContext) {
    def ^ : FutureOption[A] = future.map(Some(_)).^
  }

  implicit def toValue[A](me: FutureOption[A]): Future[Option[A]] = me.value
}

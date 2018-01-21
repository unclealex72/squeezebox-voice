import java.time.Instant
import java.util.Date

/**
  * Created by alex on 31/12/17
  **/
package object dates {

  implicit class InstantImplicits(instant: Instant) {
    def toDate: Date = new Date(instant.toEpochMilli)
  }

  implicit class DateImplicits(date: Date) {
    def toInstant: Instant = Instant.ofEpochMilli(date.getTime)
  }
}

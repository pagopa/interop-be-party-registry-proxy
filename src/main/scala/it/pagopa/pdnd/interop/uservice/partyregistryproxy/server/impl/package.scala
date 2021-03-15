package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.temporal.ChronoUnit

package object impl {
  def getInitialDelay(cronTime: String): Long = {
    val startTime: LocalTime = LocalTime.parse(cronTime)
    val today: LocalDate     = LocalDate.now()
    val next: LocalDateTime  = LocalDateTime.of(today, startTime)
    val now: LocalDateTime   = LocalDateTime.now()
    val diff: Long           = now.until(next, ChronoUnit.MILLIS)
    if (diff < 0) Math.abs(diff) else diff
  }

}

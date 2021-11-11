package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server

import java.time.LocalTime
import java.time.temporal.ChronoUnit

package object impl {

  def getInitialDelay(cronTime: String): Long = {
    val startTime: LocalTime = LocalTime.parse(cronTime)
    val now: LocalTime       = LocalTime.now()
    val diff: Long           = now.until(startTime, ChronoUnit.MILLIS)
    if (diff < 0) now.until(startTime.plusHours(24), ChronoUnit.MILLIS) else diff
  }

}

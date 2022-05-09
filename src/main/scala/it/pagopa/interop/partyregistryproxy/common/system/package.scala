package it.pagopa.interop.partyregistryproxy.common

import akka.util.Timeout
import scala.concurrent.duration._

package object system {

  implicit val timeout: Timeout = 3.seconds

}

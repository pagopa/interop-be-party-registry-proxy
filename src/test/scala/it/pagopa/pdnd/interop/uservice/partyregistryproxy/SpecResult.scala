package it.pagopa.pdnd.interop.uservice.partyregistryproxy

import akka.http.scaladsl.model.StatusCode

final case class SpecResult[A](status: StatusCode, body: A)

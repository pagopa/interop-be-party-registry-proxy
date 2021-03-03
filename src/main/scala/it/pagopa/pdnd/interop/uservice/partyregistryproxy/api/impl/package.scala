package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends DefaultJsonProtocol {
  implicit val institutionFormat: RootJsonFormat[Institution]     = jsonFormat4(Institution)
  implicit val paginatedFormat: RootJsonFormat[Paginated]         = jsonFormat2(Paginated.apply)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat4(ErrorResponse.apply)
}

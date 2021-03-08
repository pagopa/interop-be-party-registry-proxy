package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

package object impl extends DefaultJsonProtocol {

  final val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

  implicit val localDateTimeFormat: JsonFormat[LocalDate] =
    new JsonFormat[LocalDate] {
      override def write(obj: LocalDate): JsValue = JsString(obj.format(formatter))

      override def read(json: JsValue): LocalDate = json match {
        case JsString(s) =>
          Try(LocalDate.parse(s, formatter)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as java LocalDate", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val institutionFormat: RootJsonFormat[Institution]     = jsonFormat10(Institution.apply)
  implicit val paginatedFormat: RootJsonFormat[Paginated]         = jsonFormat2(Paginated.apply)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat3(ErrorResponse.apply)
}

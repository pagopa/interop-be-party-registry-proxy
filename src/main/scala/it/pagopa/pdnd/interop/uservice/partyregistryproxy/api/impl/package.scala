package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Institution, Institutions, Problem}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.{Failure, Success, Try}

package object impl extends DefaultJsonProtocol {

  final val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s) =>
          Try(UUID.fromString(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as UUID", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val localDateFormat: JsonFormat[LocalDate] =
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

  implicit val institutionFormat: RootJsonFormat[Institution]   = jsonFormat11(Institution.apply)
  implicit val institutionsFormat: RootJsonFormat[Institutions] = jsonFormat2(Institutions.apply)
  implicit val problemFormat: RootJsonFormat[Problem]           = jsonFormat3(Problem.apply)
}

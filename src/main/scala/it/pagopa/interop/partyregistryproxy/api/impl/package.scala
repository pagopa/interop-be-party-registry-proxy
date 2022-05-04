package it.pagopa.interop.partyregistryproxy.api

import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.partyregistryproxy.model._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

package object impl extends DefaultJsonProtocol {

  final val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

  implicit val localDateFormat: JsonFormat[LocalDate] =
    new JsonFormat[LocalDate] {
      override def write(obj: LocalDate): JsValue = JsString(obj.format(formatter))

      override def read(json: JsValue): LocalDate = json match {
        case JsString(s)  =>
          Try(LocalDate.parse(s, formatter)) match {
            case Success(result)    => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as java LocalDate", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val institutionFormat: RootJsonFormat[Institution]   = jsonFormat12(Institution)
  implicit val institutionsFormat: RootJsonFormat[Institutions] = jsonFormat2(Institutions)
  implicit val categoryFormat: RootJsonFormat[Category]         = jsonFormat4(Category)
  implicit val categoriesFormat: RootJsonFormat[Categories]     = jsonFormat1(Categories)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]           = jsonFormat5(Problem)

  final val serviceErrorCodePrefix: String = "010"
  final val defaultProblemType: String     = "about:blank"

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      detail = None,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )
}

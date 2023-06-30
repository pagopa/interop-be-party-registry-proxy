package it.pagopa.interop.partyregistryproxy.service.impl.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import shapeless.{Inl, Inr}
import spray.json.{
  DefaultJsonProtocol,
  JsNull,
  JsNumber,
  JsString,
  JsValue,
  JsonFormat,
  RootJsonFormat,
  deserializationError
}

trait OpenDataResponseMarshaller extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object RecordValueJsonFormat extends JsonFormat[RecordValue] {
    def write(x: RecordValue): JsValue = x match {
      case Inl(int)         => JsNumber(int)
      case Inr(Inl(string)) => JsString(string)
      case Inr(Inr(_))      => deserializationError("Write: value not admitted for RecordValue")
    }

    def read(value: JsValue): RecordValue = value match {
      case JsNumber(n) => Inl(n.toInt)
      case JsString(s) => Inr(Inl(s))
      case JsNull      => Inr(Inl(""))
      case _           => deserializationError("Read: value not admitted for RecordValue")
    }
  }

  implicit val fieldFormat: RootJsonFormat[OpenDataResponseField] = jsonFormat2(OpenDataResponseField.apply)

  implicit val institutionsResponseFormat: RootJsonFormat[OpenDataResponse] = jsonFormat2(OpenDataResponse.apply)

  implicit def fromEntityInstitutionField: FromEntityUnmarshaller[OpenDataResponseField] =
    sprayJsonUnmarshaller[OpenDataResponseField]

  implicit def fromEntityInstitutionsResponse: FromEntityUnmarshaller[OpenDataResponse] =
    sprayJsonUnmarshaller[OpenDataResponse]

}

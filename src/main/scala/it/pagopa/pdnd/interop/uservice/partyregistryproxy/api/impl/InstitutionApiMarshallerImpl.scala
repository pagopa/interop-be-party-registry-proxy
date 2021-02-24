package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

class InstitutionApiMarshallerImpl extends InstitutionApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  implicit def org: RootJsonFormat[Institution] = jsonFormat4(Institution)

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] = sprayJsonMarshaller

  implicit val toEntityMarshallerItems: ToEntityMarshaller[Seq[Institution]] = sprayJsonMarshaller

  override implicit def toEntityMarshallerPaginated: ToEntityMarshaller[Paginated] = sprayJsonMarshaller(
    jsonFormat2(Paginated.apply)
  )

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
    sprayJsonMarshaller(jsonFormat4(ErrorResponse.apply))
}

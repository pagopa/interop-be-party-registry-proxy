package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}

class InstitutionApiMarshallerImpl extends InstitutionApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] = sprayJsonMarshaller

  override implicit def toEntityMarshallerPaginated: ToEntityMarshaller[Paginated] = sprayJsonMarshaller

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = sprayJsonMarshaller
}

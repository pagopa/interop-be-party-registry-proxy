package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, InstitutionIPA}

class InstitutionApiMarshallerImpl extends InstitutionApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerInstitutionIPA: ToEntityMarshaller[InstitutionIPA] = sprayJsonMarshaller

  override implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] = sprayJsonMarshaller
}

package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Institution, Institutions, Problem}

object InstitutionApiMarshallerImpl extends InstitutionApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions] = sprayJsonMarshaller

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] = sprayJsonMarshaller

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller
}

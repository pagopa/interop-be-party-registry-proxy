package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.partyregistryproxy.api.AooApiMarshaller
import it.pagopa.interop.partyregistryproxy.model.{Institution, Institutions, Problem}

object AooApiMarshallerImpl extends AooApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions] = sprayJsonMarshaller

  override implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution] = sprayJsonMarshaller

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem
}

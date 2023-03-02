package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.partyregistryproxy.api.DatasourceApiMarshaller
import it.pagopa.interop.partyregistryproxy.model.Problem

object DatasourceApiMarshallerImpl extends DatasourceApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem
}

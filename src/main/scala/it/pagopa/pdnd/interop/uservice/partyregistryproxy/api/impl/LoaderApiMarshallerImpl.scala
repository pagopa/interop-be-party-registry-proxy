package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.LoaderApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Problem

class LoaderApiMarshallerImpl extends LoaderApiMarshaller with SprayJsonSupport {
  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller
}

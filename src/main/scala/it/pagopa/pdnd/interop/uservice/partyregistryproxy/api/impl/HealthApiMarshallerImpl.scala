package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.HealthApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Status
import spray.json._

class HealthApiMarshallerImpl extends HealthApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {
  override implicit def toEntityMarshallerStatus: ToEntityMarshaller[Status] = sprayJsonMarshaller(
    jsonFormat1(Status.apply)
  )
}

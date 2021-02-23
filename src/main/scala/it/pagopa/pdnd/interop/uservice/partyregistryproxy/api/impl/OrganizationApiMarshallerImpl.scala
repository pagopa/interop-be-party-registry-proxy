package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.OrganizationApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Error, Organization}
import spray.json._

class OrganizationApiMarshallerImpl extends OrganizationApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {
  override def toEntityMarshallerOrganization: ToEntityMarshaller[Organization] = sprayJsonMarshaller(
    jsonFormat4(Organization.apply)
  )

  override def toEntityMarshallerError: ToEntityMarshaller[Error] = sprayJsonMarshaller(jsonFormat4(Error.apply))
}

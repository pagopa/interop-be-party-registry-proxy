package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.PublicApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Organization, OrganizationError, Status}

class OrganizationApiServiceImpl extends PublicApiService {

  override def getStatus200(responseStatus: Status)(implicit
    toEntityMarshallerStatus: ToEntityMarshaller[Status]
  ): Route = ???

  override def getStatus()(implicit toEntityMarshallerStatus: ToEntityMarshaller[Status]): Route = ???

  override def getOrganizationById200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route = ???

  override def getOrganizationById400(responseError: OrganizationError)(implicit
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

  override def getOrganizationById404(responseError: OrganizationError)(implicit
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

  override def getOrganizationById(orgId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerError: ToEntityMarshaller[OrganizationError]
  ): Route = ???

}

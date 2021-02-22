package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.OrganizationApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Organization

class OrganizationApiServiceImpl extends OrganizationApiService {

  override def getOrganizationById200(responseOrganization: Organization)(
    implicit toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route = ???

  override def getOrganizationById400: Route = ???

  override def getOrganizationById404: Route = ???

  override def getOrganizationById(orgId: String)(
    implicit toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route = getOrganizationById200(Organization("id", "dn", "description", "pec"))

}

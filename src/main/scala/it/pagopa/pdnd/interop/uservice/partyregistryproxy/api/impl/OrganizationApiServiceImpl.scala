package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.OrganizationApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Organization

class OrganizationApiServiceImpl extends OrganizationApiService {
  override def getOrganizationById200(responseOrganization: Organization)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization]
  ): Route = super.getOrganizationById200(responseOrganization)

  override def getOrganizationById400(responseError: model.Error)(implicit
    toEntityMarshallerError: ToEntityMarshaller[model.Error]
  ): Route = super.getOrganizationById400(responseError)

  override def getOrganizationById404(responseError: model.Error)(implicit
    toEntityMarshallerError: ToEntityMarshaller[model.Error]
  ): Route = super.getOrganizationById404(responseError)

  override def getOrganizationById(orgId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Organization],
    toEntityMarshallerError: ToEntityMarshaller[model.Error]
  ): Route = {
    getOrganizationById200(Organization("id", "dn", "description", "pec"))
  }

}

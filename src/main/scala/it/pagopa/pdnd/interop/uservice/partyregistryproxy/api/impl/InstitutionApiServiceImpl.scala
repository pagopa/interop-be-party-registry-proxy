package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}

class InstitutionApiServiceImpl extends InstitutionApiService {

  override def getInstitutionById(orgId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Institution],
    toEntityMarshallerError: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutions(limit: Int, offset: Int)(implicit
    toEntityMarshallerPaginated: ToEntityMarshaller[Paginated],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}

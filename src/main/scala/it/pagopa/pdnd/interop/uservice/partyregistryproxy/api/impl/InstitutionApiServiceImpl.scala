package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution, Paginated}

class InstitutionApiServiceImpl extends InstitutionApiService {

  override def getInstitutionById200(responseOrganization: Institution)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Institution]
  ): Route = ???

  override def getInstitutionById400(responseError: ErrorResponse)(implicit
    toEntityMarshallerError: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutionById404(responseError: ErrorResponse)(implicit
    toEntityMarshallerError: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutionById(orgId: String)(implicit
    toEntityMarshallerOrganization: ToEntityMarshaller[Institution],
    toEntityMarshallerError: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutions200(responsePaginated: Paginated)(implicit
    toEntityMarshallerPaginated: ToEntityMarshaller[Paginated]
  ): Route = ???

  override def getInstitutions400(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutions404(responseErrorResponse: ErrorResponse)(implicit
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???

  override def getInstitutions(limit: Int, offset: Int)(implicit
    toEntityMarshallerPaginated: ToEntityMarshaller[Paginated],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = ???
}

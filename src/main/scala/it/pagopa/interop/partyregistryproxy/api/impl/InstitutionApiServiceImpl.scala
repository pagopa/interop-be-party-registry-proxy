package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.api.InstitutionApiService
import it.pagopa.interop.partyregistryproxy.api.impl.InstitutionApiResponseHandlers._
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

final case class InstitutionApiServiceImpl(institutionSearchService: IndexSearchService[Institution])
    extends InstitutionApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getInstitutionById(institutionId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving institution $institutionId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      institutionSearchService.searchById(institutionId).flatMap(_.toRight(InstitutionNotFound(institutionId)))

    getInstitutionByIdResponse[Institution](operationLabel)(getInstitutionById200)(result)
  }

  override def getInstitutionByExternalId(origin: String, originId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving institution Origin $origin OriginId $originId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      institutionSearchService
        .searchByExternalId(origin, originId)
        .flatMap(_.toRight(InstitutionByExternalNotFound(origin, originId)))

    getInstitutionByExternalIdResponse[Institution](operationLabel)(getInstitutionByExternalId200)(result)
  }

  override def searchInstitutions(search: Option[String], page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    val operationLabel = s"Searching for institutions by string: $search"
    logger.info(operationLabel)

    val result: Either[Throwable, Institutions] =
      search
        .fold(institutionSearchService.getAllItems(Map.empty, page, limit))(searchTxt =>
          institutionSearchService.searchByText(DESCRIPTION.value, searchTxt, page, limit)
        )
        .map(Institutions.tupled)

    searchInstitutionsResponse[Institutions](operationLabel)(searchInstitutions200)(result)
  }

}

package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.partyregistryproxy.api.InstitutionApiService
import it.pagopa.interop.partyregistryproxy.api.impl.InstitutionApiResponseHandlers._
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

import scala.util.Try

final case class InstitutionApiServiceImpl(institutionSearchService: IndexSearchService[Institution])
    extends InstitutionApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving institution $institutionId"
    logger.info(operationLabel)

    val result: Try[Institution] =
      institutionSearchService.searchById(institutionId).flatMap(_.toTry(InstitutionNotFound(institutionId)))

    getInstitutionByIdResponse[Institution](operationLabel)(getInstitutionById200)(result)
  }

  override def getInstitutionByExternalId(origin: String, originId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving institution Origin $origin OriginId $originId"
    logger.info(operationLabel)

    val result: Try[Institution] =
      institutionSearchService
        .searchByExternalId(origin, originId)
        .flatMap(_.toTry(InstitutionByExternalNotFound(origin, originId)))

    getInstitutionByExternalIdResponse[Institution](operationLabel)(getInstitutionByExternalId200)(result)
  }

  override def searchInstitutions(search: Option[String], page: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Searching for institutions by string: $search"
    logger.info(operationLabel)

    val result: Try[Institutions] =
      search
        .fold(institutionSearchService.getAllItems(Map.empty, page, limit))(searchTxt =>
          institutionSearchService.searchByText(DESCRIPTION.value, searchTxt, page, limit)
        )
        .map(Institutions.tupled)

    searchInstitutionsResponse[Institutions](operationLabel)(searchInstitutions200)(result)
  }

}

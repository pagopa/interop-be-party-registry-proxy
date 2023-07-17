package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.api.AooApiService
import it.pagopa.interop.partyregistryproxy.api.impl.AooApiResponseHandlers._
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

final case class AooApiServiceImpl(aooSearchService: IndexSearchService[Institution]) extends AooApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAOOByExternalId(origin: String, originId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving AOO Origin $origin OriginId $originId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      aooSearchService
        .searchByExternalId(origin, originId)
        .flatMap(_.toRight(InstitutionByExternalNotFound(origin, originId)))

    getAOOByExternalIdResponse[Institution](operationLabel)(getAOOByExternalId200)(result)
  }

  override def getAOOById(aooId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving AOO $aooId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      aooSearchService.searchById(aooId).flatMap(_.toRight(InstitutionNotFound(aooId)))

    getAOOByIdResponse[Institution](operationLabel)(getAOOById200)(result)
  }

  override def searchAOO(search: Option[String], page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    val operationLabel = s"Searching for AOO by string: $search"
    logger.info(operationLabel)

    val result: Either[Throwable, Institutions] =
      search
        .fold(aooSearchService.getAllItems(Map.empty, page, limit))(searchTxt =>
          aooSearchService.searchByText(DESCRIPTION.value, searchTxt, page, limit)
        )
        .map(Institutions.tupled)

    searchAOOResponse[Institutions](operationLabel)(searchAOO200)(result)
  }

}

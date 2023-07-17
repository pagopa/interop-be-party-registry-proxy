package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.api.UoApiService
import it.pagopa.interop.partyregistryproxy.api.impl.UoApiResponseHandlers._
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

final case class UoApiServiceImpl(uoSearchService: IndexSearchService[Institution]) extends UoApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getUOByExternalId(origin: String, originId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving UO Origin $origin OriginId $originId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      uoSearchService
        .searchByExternalId(origin, originId)
        .flatMap(_.toRight(InstitutionByExternalNotFound(origin, originId)))

    getUOByExternalIdResponse[Institution](operationLabel)(getUOByExternalId200)(result)
  }

  override def getUOById(aooId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving UO $aooId"
    logger.info(operationLabel)

    val result: Either[Throwable, Institution] =
      uoSearchService.searchById(aooId).flatMap(_.toRight(InstitutionNotFound(aooId)))

    getUOByIdResponse[Institution](operationLabel)(getUOById200)(result)
  }

  override def searchUO(search: Option[String], page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    val operationLabel = s"Searching for UO by string: $search"
    logger.info(operationLabel)

    val result: Either[Throwable, Institutions] =
      search
        .fold(uoSearchService.getAllItems(Map.empty, page, limit))(searchTxt =>
          uoSearchService.searchByText(DESCRIPTION.value, searchTxt, page, limit)
        )
        .map(Institutions.tupled)

    searchUOResponse[Institutions](operationLabel)(searchUO200)(result)
  }

}

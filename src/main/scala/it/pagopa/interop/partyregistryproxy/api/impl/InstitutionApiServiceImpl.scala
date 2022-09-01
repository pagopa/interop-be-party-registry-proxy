package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.partyregistryproxy.api.InstitutionApiService
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

import scala.util.{Failure, Success, Try}
final case class InstitutionApiServiceImpl(institutionSearchService: IndexSearchService[Institution])
    extends InstitutionApiService {

  val logger: Logger = Logger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Retrieving institution {}", institutionId)
    val result: Try[Option[Institution]] = institutionSearchService.searchById(institutionId)

    result match {
      case Success(institution) =>
        institution.fold {
          logger.error(s"Error while retrieving institution $institutionId - Institution not found")
          getInstitutionById404(problemOf(StatusCodes.NotFound, List(InstitutionNotFound(institutionId))))
        } { institution =>
          logger.info(s"Institution $institutionId retrieved")
          getInstitutionById200(institution)
        }
      case Failure(ex)          =>
        logger.error(s"Error while retrieving institution $institutionId", ex)
        complete(problemOf(StatusCodes.InternalServerError, List(InstitutionsError(ex.getMessage))))
    }
  }

  /**
   * Code: 200, Message: successful operation, DataType: Institutions
   * Code: 400, Message: Invalid input, DataType: Problem
   */
  override def searchInstitutions(search: Option[String], page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {

    logger.info("Searching for institutions with following search string = {}", search)

    val result: Try[(List[Institution], Long)] = search match {
      case Some(searchTxt) =>
        institutionSearchService
          .searchByText(DESCRIPTION.value, searchTxt, page, limit)
      case None            =>
        institutionSearchService.getAllItems(Map.empty, page, limit)
    }

    result match {
      case Success(value) => searchInstitutions200(Institutions.tupled(value))
      case Failure(ex)    =>
        logger
          .error(s"Error while searching for institutions with following search string = $search", ex)
        val error = problemOf(StatusCodes.InternalServerError, List(InstitutionsError(ex.getMessage)))
        complete(error.status, error)
    }
  }

}

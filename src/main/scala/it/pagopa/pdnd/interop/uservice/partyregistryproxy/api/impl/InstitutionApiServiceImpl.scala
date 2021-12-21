package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexSearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.InstitutionFields
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

class InstitutionApiServiceImpl(
  institutionSearchService: IndexSearchService[Institution],
  categoriesSearchService: IndexSearchService[Category]
) extends InstitutionApiService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Retrieving institution {}", institutionId)
    val result: Try[Option[Institution]] = institutionSearchService.searchById(institutionId)

    result.fold(
      ex => {
        logger.error(s"Error while retrieving institution $institutionId", ex)
        getInstitutionById400(problemOf(StatusCodes.BadRequest, "0001", ex, "Invalid"))
      },
      institution =>
        institution.fold({
          logger.error(s"Error while retrieving institution $institutionId - Institution not found")
          getInstitutionById404(problemOf(StatusCodes.NotFound, "0002", defaultMessage = "Institution not found"))
        }) { institution =>
          logger.info("Institution {} retrieved", institutionId)
          getInstitutionById200(institution)
        }
    )

  }

  /** Code: 200, Message: successful operation, DataType: Institutions
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  override def searchInstitution(search: String, page: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    logger.info("Searching for institution with following search string = {}", search)

    val result: Try[(List[Institution], Long)] =
      institutionSearchService.searchByText(InstitutionFields.DESCRIPTION, search, page, limit)

    result.fold(
      ex => {
        logger
          .error(s"Error while searching for institution with following search string = $search", ex)
        searchInstitution400(problemOf(StatusCodes.BadRequest, "0003", ex, "Invalid"))
      },
      tuple => {
        if (tuple._1.isEmpty) {
          logger.error(s"Error while searching for institution with following search string = $search - Not Found")
          searchInstitution404(problemOf(StatusCodes.NotFound, "0004", defaultMessage = "Not Found"))
        } else {
          searchInstitution200(Institutions.tupled(tuple))
        }
      }
    )

  }

  /** Code: 200, Message: successful operation, DataType: Categories
    * Code: 404, Message: Categories not found, DataType: Problem
    */
  override def getCategories()(implicit
    toEntityMarshallerCategories: ToEntityMarshaller[Categories],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Retrieving categories")
    val categories: Try[List[Category]] = categoriesSearchService.getAllItems
    categories match {
      case Success(values) if values.isEmpty =>
        getCategories404(problemOf(StatusCodes.NotFound, "0005", defaultMessage = "No category found"))
      case Success(values) => getCategories200(Categories(values))
      case Failure(ex) =>
        logger.error("Error while retrieving categories", ex)
        val error = problemOf(StatusCodes.InternalServerError, "0006", ex)
        complete(error.status, error)
    }
  }
}

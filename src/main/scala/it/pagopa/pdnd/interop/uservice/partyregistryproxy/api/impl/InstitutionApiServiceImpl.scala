package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.InstitutionFields
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

class InstitutionApiServiceImpl(
  institutionSearchService: SearchService[Institution],
  categoriesSearchService: SearchService[Category]
) extends InstitutionApiService {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Retrieving institution $institutionId")
    val result: Try[Option[Institution]] = institutionSearchService.searchById(institutionId)

    result.fold(
      ex => getInstitutionById400(Problem(Option(ex.getMessage), 400, "Invalid")),
      institution =>
        institution.fold(getInstitutionById404(Problem(detail = None, status = 404, title = "Not found"))) {
          institution =>
            logger.info(s"Institution $institutionId retrieved")
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
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Searching for institution with $search")

    val result: Try[(List[Institution], Long)] =
      institutionSearchService.searchByText(InstitutionFields.DESCRIPTION, search, page, limit)

    result.fold(
      ex => searchInstitution400(Problem(Option(ex.getMessage), 400, "Invalid")),
      tuple => {
        if (tuple._1.isEmpty)
          searchInstitution404(Problem(None, 404, "Not found"))
        else {
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
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val categories: Try[List[Category]] = categoriesSearchService.getAllItems
    categories match {
      case Success(values) if values.isEmpty => getCategories404(Problem(None, 404, "No category found"))
      case Success(values)                   => getCategories200(Categories(values))
      case Failure(ex) =>
        complete(StatusCodes.InternalServerError, Problem(Option(ex.getMessage), 500, "Something went wrong"))
    }
  }
}

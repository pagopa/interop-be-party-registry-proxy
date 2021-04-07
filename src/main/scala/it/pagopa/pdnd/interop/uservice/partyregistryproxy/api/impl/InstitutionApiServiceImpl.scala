package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Institution, Institutions, Problem}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

class InstitutionApiServiceImpl(searchService: SearchService) extends InstitutionApiService {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info(s"Retrieving institution $institutionId")
    val result: Try[Option[Institution]] = searchService.searchById(institutionId)

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
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    logger.info(s"Searching for institution with $search")

    val result: Try[(List[Institution], Long)] = searchService.searchByDescription(search, page, limit)

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

}

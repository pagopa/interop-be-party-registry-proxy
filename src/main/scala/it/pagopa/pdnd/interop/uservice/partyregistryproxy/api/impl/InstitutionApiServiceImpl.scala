package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Institution, Institutions, Problem}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.LuceneService
import org.slf4j.{Logger, LoggerFactory}

class InstitutionApiServiceImpl(luceneService: LuceneService) extends InstitutionApiService {
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
    val result: Option[Institution] = luceneService.searchById(institutionId)

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")

    result.fold(getInstitutionById404(errorResponse)) { institution =>
      logger.info(s"Institution $institutionId retrieved")
      getInstitutionById200(institution)
    }

  }

  /** Code: 200, Message: successful operation, DataType: Institutions
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  override def searchInstitution(search: String, offset: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    logger.info(s"Searching for institution with $search")
    val institutions: List[Institution] = luceneService.searchByDescription(search, limit)

    searchInstitution200(Institutions(institutions))

  }

}

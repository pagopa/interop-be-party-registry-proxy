package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.pagopa.interop.partyregistryproxy.api.InstitutionApiService
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import com.typesafe.scalalogging.Logger

import scala.util.Try

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

    result.fold(
      ex => {
        logger.error(s"Error while retrieving institution $institutionId", ex)
        getInstitutionById400(problemOf(StatusCodes.BadRequest, InvalidGetInstitutionRequest))
      },
      institution =>
        institution.fold({
          logger.error(s"Error while retrieving institution $institutionId - Institution not found")
          getInstitutionById404(problemOf(StatusCodes.NotFound, InstitutionNotFound))
        }) { institution =>
          logger.info("Institution {} retrieved", institutionId)
          getInstitutionById200(institution)
        }
    )

  }

  /**
   * Code: 200, Message: successful operation, DataType: Institutions
   * Code: 400, Message: Invalid ID supplied, DataType: Problem
   * Code: 404, Message: Institution not found, DataType: Problem
   */
  override def searchInstitution(search: Option[String], page: Option[Int], limit: Option[Int])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {
    logger.info("Searching for institution with following search string = {}", search)

    val result: Try[(List[Institution], Long)] =
      search
        .map(searchTxt =>
          institutionSearchService
            .searchByText(DESCRIPTION.value, searchTxt, page.getOrElse(defaultPage), limit.getOrElse(defaultLimit))
        )
        .getOrElse(
          institutionSearchService.getAllItems(Map.empty, page.getOrElse(defaultPage), limit.getOrElse(defaultLimit))
        )

    result.fold(
      ex => {
        logger
          .error(s"Error while searching for institution with following search string = $search", ex)
        searchInstitution400(problemOf(StatusCodes.BadRequest, InvalidSearchInstitutionRequest))
      },
      tuple => {
        if (tuple._1.isEmpty) {
          logger.error(s"Error while searching for institution with following search string = $search - Not Found")
          searchInstitution404(problemOf(StatusCodes.NotFound, InstitutionsNotFound))
        } else {
          searchInstitution200(Institutions.tupled(tuple))
        }
      }
    )

  }

}

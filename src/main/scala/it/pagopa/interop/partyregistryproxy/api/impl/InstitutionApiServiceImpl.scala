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

  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
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

  override def getInstitutionByExternalId(origin: String, originId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    logger.info(s"Retrieving institution origin=$origin/originId=$originId")
    val result: Try[Option[Institution]] = institutionSearchService.searchByExternalId(origin, originId)

    result match {
      case Success(institution) =>
        institution.fold {
          logger.error(s"Error while retrieving institution origin=$origin/originId=$originId - Institution not found")
          getInstitutionById404(
            problemOf(StatusCodes.NotFound, List(InstitutionNotFound(s"origin=$origin/originId=$originId")))
          )
        } { institution =>
          logger.info(s"Institution origin=$origin/originId=$originId retrieved")
          getInstitutionById200(institution)
        }
      case Failure(ex)          =>
        logger.error(s"Error while retrieving institution origin=$origin/originId=$originId", ex)
        complete(problemOf(StatusCodes.InternalServerError, List(InstitutionsError(ex.getMessage))))
    }
  }

  override def searchInstitutions(search: Option[String], page: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions],
    contexts: Seq[(String, String)]
  ): Route = {

    logger.info("Searching for institutions with following search string = {}", search)

    val result: Try[(List[Institution], Long)] =
      search.fold(institutionSearchService.getAllItems(Map.empty, page, limit))(searchTxt =>
        institutionSearchService
          .searchByText(DESCRIPTION.value, searchTxt, page, limit)
      )

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

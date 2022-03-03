package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import it.pagopa.interop.partyregistryproxy.api.CategoryApiService
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.ORIGIN
import it.pagopa.interop.partyregistryproxy.common.util.{SearchField, createCategoryId}
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

final case class CategoryApiServiceImpl(categoriesSearchService: IndexSearchService[Category])
    extends CategoryApiService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: Categories
    * Code: 404, Message: Categories not found, DataType: Problem
    */
  override def getCategories(origin: Option[String])(implicit
    toEntityMarshallerCategories: ToEntityMarshaller[Categories],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Retrieving categories")

    val filters: Map[SearchField, String] = getCategoryFilters(origin)
    val categories: Try[List[Category]]   = categoriesSearchService.getAllItems(filters)
    categories match {
      case Success(values) if values.isEmpty =>
        getCategories404(problemOf(StatusCodes.NotFound, CategoriesNotFound))
      case Success(values) => getCategories200(Categories(values))
      case Failure(ex) =>
        logger.error(s"Error while retrieving categories - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, CategoriesError)
        complete(error.status, error)
    }
  }

  private def getCategoryFilters(origin: Option[String]): Map[SearchField, String] = {
    origin.fold(Map.empty[SearchField, String])(o => Map(ORIGIN -> o))
  }

  /** Code: 200, Message: successful operation, DataType: Category
    * Code: 400, Message: Invalid code supplied, DataType: Problem
    * Code: 404, Message: Category not found, DataType: Problem
    */
  override def getCategory(origin: String, code: String)(implicit
    toEntityMarshallerCategory: ToEntityMarshaller[Category],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    logger.info(s"Retrieving category $code")

    val id: String = createCategoryId(origin = origin, code = code)

    val category: Try[Option[Category]] = categoriesSearchService.searchById(id)

    category match {
      case Success(value) =>
        val error = problemOf(StatusCodes.NotFound, CategoryNotFound(code))
        value.fold(getCategory404(error))(getCategory200)
      case Failure(ex) =>
        logger.error(s"Error while retrieving category $code - ${ex.getMessage}")
        val error = problemOf(StatusCodes.BadRequest, CategoriesError)
        getCategory400(error)
    }
  }

}

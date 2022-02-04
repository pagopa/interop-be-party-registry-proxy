package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.CategoryApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.{CategoryField, SearchField, createCategoryId}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.IndexSearchService
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
        logger.error("Error while retrieving categories", ex)
        val error = problemOf(StatusCodes.InternalServerError, CategoriesError)
        complete(error.status, error)
    }
  }

  private def getCategoryFilters(origin: Option[String]): Map[SearchField, String] = {
    origin.fold(Map.empty[SearchField, String])(o => Map(CategoryField.ORIGIN -> o))
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
        logger.error(s"Error while retrieving category $code", ex)
        val error = problemOf(StatusCodes.BadRequest, CategoriesError)
        getCategory400(error)
    }
  }

}

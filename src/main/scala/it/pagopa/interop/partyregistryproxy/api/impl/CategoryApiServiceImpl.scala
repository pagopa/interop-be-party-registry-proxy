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
import com.typesafe.scalalogging.Logger
import scala.util.{Failure, Success, Try}

final case class CategoryApiServiceImpl(categoriesSearchService: IndexSearchService[Category])
    extends CategoryApiService {

  val logger: Logger = Logger(this.getClass)

  /**
   * Code: 200, Message: successful operation, DataType: Categories
   */
  override def getCategories(origin: Option[String], page: Option[Int], limit: Option[Int])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerCategories: ToEntityMarshaller[Categories]
  ): Route = {

    import CategoryApiMarshallerImpl.toEntityMarshallerProblem

    logger.info("Retrieving categories")

    val filters: Map[SearchField, String]       = getCategoryFilters(origin)
    val categories: Try[(List[Category], Long)] =
      categoriesSearchService.getAllItems(filters, page.getOrElse(defaultPage), limit.getOrElse(defaultLimit))
    categories match {
      case Success(values) => getCategories200(Categories(values._1, values._2))
      case Failure(ex)     =>
        logger.error(s"Error while retrieving categories", ex)
        val error = problemOf(StatusCodes.InternalServerError, CategoriesError(ex.getMessage))
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
    contexts: Seq[(String, String)],
    toEntityMarshallerCategory: ToEntityMarshaller[Category],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    logger.info(s"Retrieving category $code")

    val id: String = createCategoryId(origin = origin, code = code)

    val category: Try[Option[Category]] = categoriesSearchService.searchById(id)

    category match {
      case Success(value) =>
        val error = problemOf(StatusCodes.NotFound, CategoryNotFound(code))
        value.fold {
          logger.error(s"Error while retrieving category $code - Category not found")
          getCategory404(error)
        } {
          logger.info(s"Category $code retrieved")
          getCategory200
        }
      case Failure(ex)    =>
        logger.error(s"Error while retrieving category $code", ex)
        val error = problemOf(StatusCodes.InternalServerError, CategoriesError(ex.getMessage))
        complete(error.status, error)
    }
  }

}

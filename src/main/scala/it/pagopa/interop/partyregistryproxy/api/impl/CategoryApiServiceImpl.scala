package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.api.CategoryApiService
import it.pagopa.interop.partyregistryproxy.api.impl.CategoryApiResponseHandlers._
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.ORIGIN
import it.pagopa.interop.partyregistryproxy.common.util.{SearchField, createCategoryId}
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService

final case class CategoryApiServiceImpl(categoriesSearchService: IndexSearchService[Category])
    extends CategoryApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getCategories(origin: Option[String], page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerCategories: ToEntityMarshaller[Categories],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving categories for Origin ${origin.getOrElse("-")} Page $page Limit $limit"
    logger.info(operationLabel)

    val filters: Map[SearchField, String]         = getCategoryFilters(origin)
    val categories: Either[Throwable, Categories] =
      categoriesSearchService
        .getAllItems(filters, page, limit)
        .map { case (items, totalCount) => Categories(items, totalCount) }

    getCategoriesResponse[Categories](operationLabel)(getCategories200)(categories)

  }

  private def getCategoryFilters(origin: Option[String]): Map[SearchField, String] =
    origin.fold(Map.empty[SearchField, String])(o => Map(ORIGIN -> o))

  override def getCategory(origin: String, code: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerCategory: ToEntityMarshaller[Category],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val operationLabel = s"Retrieving category for Origin $origin Code $code"
    logger.info(operationLabel)

    val id: String = createCategoryId(origin = origin, code = code)

    val category: Either[Throwable, Category] =
      categoriesSearchService.searchById(id).flatMap(_.toRight(CategoryNotFound(code)))

    getCategoryResponse[Category](operationLabel)(getCategory200)(category)
  }

}

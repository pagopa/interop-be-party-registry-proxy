package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.api.DatasourceApiService
import it.pagopa.interop.partyregistryproxy.api.impl.DatasourceApiResponseHandlers.reloadAllDataResponse
import it.pagopa.interop.partyregistryproxy.common.util.DataSourceOps.loadOpenData
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.service.{IndexWriterService, OpenDataService}

import scala.concurrent.ExecutionContext

final class DatasourceApiServiceImpl(
  openDataService: OpenDataService,
  institutionsWriterService: IndexWriterService[Institution],
  aooWriterService: IndexWriterService[Institution],
  uoWriterService: IndexWriterService[Institution],
  categoriesWriterService: IndexWriterService[Category]
)(blockingEc: ExecutionContext)
    extends DatasourceApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def reloadAllData()(implicit contexts: Seq[(String, String)]): Route = {
    val operationLabel = s"Reloading data from all sources"
    logger.info(operationLabel)

    val result =
      loadOpenData(
        openDataService,
        institutionsWriterService,
        aooWriterService,
        uoWriterService,
        categoriesWriterService,
        blockingEc
      )

    onComplete(result) { reloadAllDataResponse[Unit](operationLabel)(_ => reloadAllData204) }

  }

}

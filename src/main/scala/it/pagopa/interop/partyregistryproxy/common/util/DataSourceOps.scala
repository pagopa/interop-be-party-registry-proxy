package it.pagopa.interop.partyregistryproxy.common.util

import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.{IndexWriterService, OpenDataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object DataSourceOps {

  def loadOpenData(
    openDataService: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category],
    blockingEc: ExecutionContext
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] = {
    implicit val ec: ExecutionContext = blockingEc
    logger.info(s"Loading open data")
    val result: Future[Unit]          = for {
      institutions <- openDataService.getAllInstitutions
      _ = println(institutions.size)
      _          <- loadInstitutions(institutionsIndexWriterService, institutions)
      categories <- openDataService.getAllCategories
      _          <- loadCategories(categoriesIndexWriterService, categories)
    } yield ()

    result.onComplete {
      case Success(_)  => logger.info(s"Open data committed")
      case Failure(ex) => logger.error(s"Error trying to populate index", ex)
    }

    result
  }

  private def loadInstitutions(
    institutionsIndexWriterService: IndexWriterService[Institution],
    institutions: List[Institution]
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] =
    Future.fromTry {
      logger.info("Loading institutions index from iPA")
      institutionsIndexWriterService.resource { writer =>
        institutionsIndexWriterService.adds(institutions)(writer).map(_ => logger.info(s"Institutions inserted"))
      }
    }

  private def loadCategories(
    categoriesIndexWriterService: IndexWriterService[Category],
    categories: List[Category]
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] =
    Future.fromTry {
      logger.info("Loading categories index from iPA")
      categoriesIndexWriterService.resource { writer =>
        categoriesIndexWriterService.adds(categories)(writer).map(_ => logger.info(s"Categories inserted"))
      }
    }

}

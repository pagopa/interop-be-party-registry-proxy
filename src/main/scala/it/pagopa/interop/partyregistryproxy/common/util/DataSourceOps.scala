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
    mockOpenDataServiceImpl: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category],
    blockingEc: ExecutionContext
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] = {
    implicit val ec: ExecutionContext = blockingEc
    logger.info(s"Loading open data")
    val result: Future[Unit]          = for {
      institutions     <- openDataService.getAllInstitutions
      mockInstitutions <- mockOpenDataServiceImpl.getAllInstitutions
      _                <- loadInstitutions(institutionsIndexWriterService, institutions ++ mockInstitutions)
      categories       <- openDataService.getAllCategories
      mockCategories   <- mockOpenDataServiceImpl.getAllCategories
      _                <- loadCategories(categoriesIndexWriterService, categories ++ mockCategories)
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
      for {
        _ <- institutionsIndexWriterService.adds(institutions)
        _ = logger.info(s"Institutions inserted")
        _ <- institutionsIndexWriterService.commit()
      } yield ()
    }

  private def loadCategories(
    categoriesIndexWriterService: IndexWriterService[Category],
    categories: List[Category]
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] =
    Future.fromTry {
      logger.info("Loading categories index from iPA")
      for {
        _ <- categoriesIndexWriterService.adds(categories)
        _ = logger.info(s"Categories inserted")
        _ <- categoriesIndexWriterService.commit()
      } yield ()
    }

}

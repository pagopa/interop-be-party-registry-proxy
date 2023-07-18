package it.pagopa.interop.partyregistryproxy.common.util

import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.InstitutionKind._
import it.pagopa.interop.partyregistryproxy.service.{IndexWriterService, OpenDataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object DataSourceOps {

  def loadOpenData(
    openDataService: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    aooWriterService: IndexWriterService[Institution],
    uoWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category],
    blockingEc: ExecutionContext
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] = {
    implicit val ec: ExecutionContext = blockingEc
    logger.info(s"Loading open data")
    val result: Future[Unit]          = for {
      institutions <- openDataService.getAllInstitutions(Map.empty, Agency)
      institutionsDetails = institutions.map(i => i.originId -> InstitutionDetails(i.category, i.kind)).toMap
      _          <- loadInstitutions(institutionsIndexWriterService, institutions)
      aoo        <- openDataService.getAllInstitutions(institutionsDetails, AOO)
      _          <- loadInstitutions(aooWriterService, aoo)
      uo         <- openDataService.getAllInstitutions(institutionsDetails, UO)
      _          <- loadInstitutions(uoWriterService, uo)
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
    indexWriterService: IndexWriterService[Institution],
    institutions: List[Institution]
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: Seq[(String, String)]): Future[Unit] =
    Future.fromTry {
      logger.info("Loading institutions index from iPA")
      indexWriterService.resource { writer =>
        indexWriterService.adds(institutions)(writer).map(_ => logger.info(s"Institutions inserted"))
      }
    }

  private def loadCategories(indexWriterService: IndexWriterService[Category], categories: List[Category])(implicit
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    contexts: Seq[(String, String)]
  ): Future[Unit] =
    Future.fromTry {
      logger.info("Loading categories index from iPA")
      indexWriterService.resource { writer =>
        indexWriterService.adds(categories)(writer).map(_ => logger.info(s"Categories inserted"))
      }
    }

}

package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.{OpenDataService, SearchService}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object OpenDataLoader {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def loadOpenData(
    openDataService: OpenDataService,
    institutionsSearchService: SearchService[Institution],
    categoriesSearchService: SearchService[Category]
  )(implicit ec: ExecutionContext): Unit = {
    logger.info(s"Loading open data")
    val result = futureLoadOpenData(openDataService, institutionsSearchService, categoriesSearchService)

    result.onComplete {
      case Success(_) => logger.info(s"Open data committed")
      case Failure(ex) =>
        logger.error(s"Error trying to populate index, due: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }

  def futureLoadOpenData(
    openDataService: OpenDataService,
    institutionsSearchService: SearchService[Institution],
    categoriesSearchService: SearchService[Category]
  )(implicit ec: ExecutionContext) = {
    for {
      institutions <- openDataService.getAllInstitutions
      _            <- loadInstitutions(institutionsSearchService, institutions)
      categories   <- openDataService.getAllCategories
      _            <- loadCategories(categoriesSearchService, categories)
    } yield ()
  }

  private def loadInstitutions(
    institutionsSearchService: SearchService[Institution],
    institutions: List[Institution]
  ): Future[Long] = Future.fromTry {
    logger.info("Loading institutions index from iPA")
    for {
      _ <- institutionsSearchService.adds(institutions)
      _ = logger.info(s"Institutions inserted")
    } yield institutionsSearchService.commit()
  }

  private def loadCategories(
    categoriesSearchService: SearchService[Category],
    categories: List[Category]
  ): Future[Long] = Future.fromTry {
    logger.info("Loading categories index from iPA")
    for {
      _ <- categoriesSearchService.adds(categories)
      _ = logger.info(s"Categories inserted")
    } yield categoriesSearchService.commit()
  }
}

package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.{HealthApi, InstitutionApi}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{
  ApplicationConfiguration,
  Authenticator,
  CorsSupport,
  actorSystem,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.{
  CategorySearchServiceImpl,
  InstitutionSearchServiceImpl,
  OpenDataServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.{OpenDataService, SearchService}
import kamon.Kamon
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App with CorsSupport {

  val logger = LoggerFactory.getLogger(this.getClass)

  Kamon.init()

  final val http: HttpExt = Http()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val institutionsSearchService: SearchService[Institution] = InstitutionSearchServiceImpl
  val categoriesSearchService: SearchService[Category]      = CategorySearchServiceImpl
  val openDataService: OpenDataService                      = OpenDataServiceImpl(http)(actorSystem, executionContext)

  locally {
    val _ = loadOpenData(openDataService, institutionsSearchService, categoriesSearchService)
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(institutionsSearchService, categoriesSearchService),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val initialDelay: Long = getInitialDelay(ApplicationConfiguration.cronTime)

  val controller = new Controller(healthApi, institutionApi)

  logger.error(s"Started build info = ${buildinfo.BuildInfo.toString}")

  val bindingFuture =
    http.newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

  def loadOpenData(
    openDataService: OpenDataService,
    institutionsSearchService: SearchService[Institution],
    categoriesSearchService: SearchService[Category]
  ): Unit = {
    logger.info(s"Loading open data")
    val result: Future[Unit] = for {
      institutions <- openDataService.getAllInstitutions
      _            <- loadInstitutions(institutionsSearchService, institutions)
      categories   <- openDataService.getAllCategories
      _            <- loadCategories(categoriesSearchService, categories)
    } yield ()

    result.onComplete {
      case Success(_) => logger.info(s"Open data committed")
      case Failure(ex) =>
        logger.error(s"Error trying to populate index, due: ${ex.getMessage}")
        ex.printStackTrace()
    }
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

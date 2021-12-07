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
  CategoryIndexSearchServiceImpl,
  CategoryIndexWriterServiceImpl,
  InstitutionIndexSearchServiceImpl,
  InstitutionIndexWriterServiceImpl,
  OpenDataServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.{
  IndexSearchService,
  IndexWriterService,
  OpenDataService
}
import kamon.Kamon
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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

  val institutionsWriterService: IndexWriterService[Institution] = InstitutionIndexWriterServiceImpl
  val categoriesWriterService: IndexWriterService[Category]      = CategoryIndexWriterServiceImpl
  val openDataService: OpenDataService                           = OpenDataServiceImpl(http)(actorSystem, executionContext)

  locally {
    val _ = loadOpenData(openDataService, institutionsWriterService, categoriesWriterService)
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val institutionsSearchService: IndexSearchService[Institution] = InstitutionIndexSearchServiceImpl
  val categoriesSearchService: IndexSearchService[Category]      = CategoryIndexSearchServiceImpl

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
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category]
  ): Unit = {
    logger.info(s"Loading open data")
    val result: Future[Unit] = for {
      institutions <- openDataService.getAllInstitutions
      _            <- loadInstitutions(institutionsIndexWriterService, institutions)
      categories   <- openDataService.getAllCategories
      _            <- loadCategories(categoriesIndexWriterService, categories)
    } yield ()

    result.onComplete {
      case Success(_) => logger.info(s"Open data committed")
      case Failure(ex) =>
        logger.error(s"Error trying to populate index, due: ${ex.getMessage}")
        ex.printStackTrace()
    }

    Await.result(result, Duration.Inf)
  }

  private def loadInstitutions(
    institutionsIndexWriterService: IndexWriterService[Institution],
    institutions: List[Institution]
  ): Future[Unit] = Future.fromTry {
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
  ): Future[Unit] = Future.fromTry {
    logger.info("Loading categories index from iPA")
    for {
      _ <- categoriesIndexWriterService.adds(categories)
      _ = logger.info(s"Categories inserted")
      _ <- categoriesIndexWriterService.commit()
    } yield ()
  }
}

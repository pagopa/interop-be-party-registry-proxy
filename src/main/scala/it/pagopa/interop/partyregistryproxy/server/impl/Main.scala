package it.pagopa.interop.partyregistryproxy.server.impl

import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
import akka.management.scaladsl.AkkaManagement
import it.pagopa.interop.partyregistryproxy.api.impl.{
  CategoryApiMarshallerImpl,
  CategoryApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.interop.partyregistryproxy.api.{CategoryApi, HealthApi, InstitutionApi}
import it.pagopa.interop.partyregistryproxy.common.system.{
  ApplicationConfiguration,
  Authenticator,
  CorsSupport,
  actorSystem,
  classicActorSystem,
  executionContext
}
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.server.Controller
import it.pagopa.interop.partyregistryproxy.service.impl.{
  CategoryIndexSearchServiceImpl,
  CategoryIndexWriterServiceImpl,
  IPAOpenDataServiceImpl,
  InstitutionIndexSearchServiceImpl,
  InstitutionIndexWriterServiceImpl,
  MockOpenDataServiceImpl
}
import it.pagopa.interop.partyregistryproxy.service.{IndexSearchService, IndexWriterService, OpenDataService}
import kamon.Kamon
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App with CorsSupport {

  private val logger = LoggerFactory.getLogger(this.getClass)

  Kamon.init()

  final val http: HttpExt = Http()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val institutionsWriterService: IndexWriterService[Institution] = InstitutionIndexWriterServiceImpl
  val categoriesWriterService: IndexWriterService[Category]      = CategoryIndexWriterServiceImpl
  val openDataService: OpenDataService                           = IPAOpenDataServiceImpl(http)(actorSystem, executionContext)
  val mockOpenDataServiceImpl: MockOpenDataServiceImpl =
    MockOpenDataServiceImpl(
      institutionsMockOpenDataUrl = ApplicationConfiguration.institutionsMockOpenDataUrl,
      categoriesMockOpenDataUrl = ApplicationConfiguration.categoriesMockOpenDataUrl,
      mockOrigin = ApplicationConfiguration.mockOrigin,
      http = http
    )(actorSystem, executionContext)

  locally {
    val _ =
      loadOpenData(openDataService, mockOpenDataServiceImpl, institutionsWriterService, categoriesWriterService)
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val institutionsSearchService: IndexSearchService[Institution] = InstitutionIndexSearchServiceImpl
  val categoriesSearchService: IndexSearchService[Category]      = CategoryIndexSearchServiceImpl

  val institutionApi: InstitutionApi = new InstitutionApi(
    InstitutionApiServiceImpl(institutionsSearchService),
    InstitutionApiMarshallerImpl,
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val categoryApi: CategoryApi = new CategoryApi(
    CategoryApiServiceImpl(categoriesSearchService),
    CategoryApiMarshallerImpl,
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val controller = new Controller(category = categoryApi, health = healthApi, institution = institutionApi)

  logger.error(s"Started build info = ${buildinfo.BuildInfo.toString}")

  val bindingFuture =
    http.newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

  def loadOpenData(
    openDataService: OpenDataService,
    mockOpenDataServiceImpl: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category]
  ): Unit = {
    logger.info(s"Loading open data")
    val result: Future[Unit] = for {
      institutions     <- openDataService.getAllInstitutions
      mockInstitutions <- mockOpenDataServiceImpl.getAllInstitutions
      _                <- loadInstitutions(institutionsIndexWriterService, institutions ++ mockInstitutions)
      categories       <- openDataService.getAllCategories
      mockCategories   <- mockOpenDataServiceImpl.getAllCategories
      _                <- loadCategories(categoriesIndexWriterService, categories ++ mockCategories)
    } yield ()

    result.onComplete {
      case Success(_) => logger.info(s"Open data committed")
      case Failure(ex) =>
        logger.error(s"Error trying to populate index - ${ex.getMessage}")
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

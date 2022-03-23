package it.pagopa.interop.partyregistryproxy.server.impl

import akka.actor.CoordinatedShutdown
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
  PagopaOpenDataServiceImpl
}
import it.pagopa.interop.partyregistryproxy.service.{IndexSearchService, IndexWriterService, OpenDataService}
import kamon.Kamon
import org.slf4j.LoggerFactory
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jose.proc.SecurityContext
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.commons.utils.TypeConversions._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case object StartupErrorShutdown extends CoordinatedShutdown.Reason

object Main extends App with CorsSupport {

  private val logger = LoggerFactory.getLogger(this.getClass)

  Kamon.init()

  val dependenciesLoaded: Future[JWTReader] = for {
    keyset <- JWTConfiguration.jwtReader.loadKeyset().toFuture
    jwtValidator = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset: Map[KID, SerializedKey] = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
        getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
    }
  } yield jwtValidator

  dependenciesLoaded.transformWith {
    case Success(jwtValidator) => launchApp(jwtValidator)
    case Failure(ex) =>
      classicActorSystem.log.error("Startup error: {}", ex.getMessage)
      classicActorSystem.log.error(ex.getStackTrace.mkString("\n"))
      CoordinatedShutdown(classicActorSystem).run(StartupErrorShutdown)
  }

  private def launchApp(jwtReader: JWTReader): Future[Http.ServerBinding] = {
    val http: HttpExt = Http()

    val healthApi: HealthApi = new HealthApi(
      new HealthApiServiceImpl(),
      new HealthApiMarshallerImpl(),
      SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator)
    )

    val institutionsWriterService: IndexWriterService[Institution] = InstitutionIndexWriterServiceImpl
    val categoriesWriterService: IndexWriterService[Category]      = CategoryIndexWriterServiceImpl
    val openDataService: OpenDataService                           = IPAOpenDataServiceImpl(http)(actorSystem, executionContext)
    val pagopaDataService: OpenDataService                         = PagopaOpenDataServiceImpl

    locally {
      val _ = loadOpenData(openDataService, pagopaDataService, institutionsWriterService, categoriesWriterService)
      val _ = AkkaManagement.get(classicActorSystem).start()
    }

    val institutionsSearchService: IndexSearchService[Institution] = InstitutionIndexSearchServiceImpl
    val categoriesSearchService: IndexSearchService[Category]      = CategoryIndexSearchServiceImpl

    val institutionApi: InstitutionApi = new InstitutionApi(
      InstitutionApiServiceImpl(institutionsSearchService),
      InstitutionApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

    val categoryApi: CategoryApi = new CategoryApi(
      CategoryApiServiceImpl(categoriesSearchService),
      CategoryApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

    val controller = new Controller(category = categoryApi, health = healthApi, institution = institutionApi)

    logger.info(s"Started build info = ${buildinfo.BuildInfo.toString}")

    http.newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))
  }

  def loadOpenData(
    openDataService: OpenDataService,
    pagopaDataService: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category]
  ): Unit = {
    logger.info(s"Loading open data")
    val result: Future[Unit] = for {
      institutions       <- openDataService.getAllInstitutions
      pagopaInstitutions <- pagopaDataService.getAllInstitutions
      _                  <- loadInstitutions(institutionsIndexWriterService, institutions ++ pagopaInstitutions)
      categories         <- openDataService.getAllCategories
      pagopaCategories   <- pagopaDataService.getAllCategories
      _                  <- loadCategories(categoriesIndexWriterService, categories ++ pagopaCategories)
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

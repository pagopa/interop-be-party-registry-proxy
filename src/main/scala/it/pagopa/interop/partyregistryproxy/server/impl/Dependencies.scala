package it.pagopa.interop.partyregistryproxy.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.partyregistryproxy.api.impl.{
  CategoryApiMarshallerImpl,
  CategoryApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.interop.partyregistryproxy.api.{CategoryApi, HealthApi, InstitutionApi}
import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.impl.{
  CategoryIndexSearchServiceImpl,
  CategoryIndexWriterServiceImpl,
  IPAOpenDataServiceImpl,
  InstitutionIndexSearchServiceImpl,
  InstitutionIndexWriterServiceImpl,
  MockOpenDataServiceImpl
}
import it.pagopa.interop.partyregistryproxy.service.{IndexWriterService, OpenDataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

trait Dependencies {

  def loadOpenData(
    openDataService: OpenDataService,
    mockOpenDataServiceImpl: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category],
    blockingEc: ExecutionContextExecutor
  )(implicit logger: Logger): Future[Unit] = {
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
  )(implicit logger: Logger): Future[Unit] = Future.fromTry {
    logger.info("Loading institutions index from iPA")
    for {
      _ <- institutionsIndexWriterService.adds(institutions)
      _ = logger.info(s"Institutions inserted")
      _ <- institutionsIndexWriterService.commit()
    } yield ()
  }

  private def loadCategories(categoriesIndexWriterService: IndexWriterService[Category], categories: List[Category])(
    implicit logger: Logger
  ): Future[Unit] = Future.fromTry {
    logger.info("Loading categories index from iPA")
    for {
      _ <- categoriesIndexWriterService.adds(categories)
      _ = logger.info(s"Categories inserted")
      _ <- categoriesIndexWriterService.commit()
    } yield ()
  }

  def http()(implicit actorSystem: ActorSystem[_]): HttpExt = Http()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator),
    loggingEnabled = false
  )

  val institutionsWriterService: IndexWriterService[Institution] = InstitutionIndexWriterServiceImpl
  val categoriesWriterService: IndexWriterService[Category]      = CategoryIndexWriterServiceImpl
  def openDataService()(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): OpenDataService                 =
    IPAOpenDataServiceImpl(http())
  def mockOpenDataServiceImpl()(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): MockOpenDataServiceImpl =
    MockOpenDataServiceImpl(
      institutionsMockOpenDataUrl = ApplicationConfiguration.institutionsMockOpenDataUrl,
      categoriesMockOpenDataUrl = ApplicationConfiguration.categoriesMockOpenDataUrl,
      mockOrigin = ApplicationConfiguration.mockOrigin,
      http = http()
    )

  def institutionApi()(implicit jwtReader: JWTReader): InstitutionApi = new InstitutionApi(
    InstitutionApiServiceImpl(InstitutionIndexSearchServiceImpl),
    InstitutionApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def categoryApi()(implicit jwtReader: JWTReader): CategoryApi = new CategoryApi(
    CategoryApiServiceImpl(CategoryIndexSearchServiceImpl),
    CategoryApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def createJwtReader(keyset: Map[KID, SerializedKey]): JWTReader = new DefaultJWTReader with PublicKeysHolder {
    var publicKeyset: Map[KID, SerializedKey] = keyset

    override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
      getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
  }
}

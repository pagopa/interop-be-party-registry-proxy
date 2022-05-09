package it.pagopa.interop.partyregistryproxy.server.impl

import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
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
import it.pagopa.interop.partyregistryproxy.service.{IndexSearchService, IndexWriterService, OpenDataService}
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{KID, PublicKeysHolder, SerializedKey}
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jose.proc.SecurityContext
import it.pagopa.interop.commons.utils.AkkaUtils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import com.typesafe.scalalogging.Logger
import akka.actor.typed.ActorSystem

trait Dependencies {

  def loadOpenData(
    openDataService: OpenDataService,
    mockOpenDataServiceImpl: OpenDataService,
    institutionsIndexWriterService: IndexWriterService[Institution],
    categoriesIndexWriterService: IndexWriterService[Category]
  )(implicit ec: ExecutionContext, logger: Logger): Unit = {
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
      case Success(_)  => logger.info(s"Open data committed")
      case Failure(ex) => logger.error(s"Error trying to populate index", ex)
    }

    Await.result(result, Duration.Inf)
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
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator)
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

  val institutionsSearchService: IndexSearchService[Institution] = InstitutionIndexSearchServiceImpl
  val categoriesSearchService: IndexSearchService[Category]      = CategoryIndexSearchServiceImpl

  def institutionApi()(implicit jwtReader: JWTReader): InstitutionApi = new InstitutionApi(
    InstitutionApiServiceImpl(institutionsSearchService),
    InstitutionApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def categoryApi()(implicit jwtReader: JWTReader): CategoryApi = new CategoryApi(
    CategoryApiServiceImpl(categoriesSearchService),
    CategoryApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def createJwtReader(keyset: Map[KID, SerializedKey]): JWTReader = new DefaultJWTReader with PublicKeysHolder {
    var publicKeyset: Map[KID, SerializedKey] = keyset

    override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
      getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
  }
}

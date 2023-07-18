package it.pagopa.interop.partyregistryproxy.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils
import it.pagopa.interop.partyregistryproxy.api.impl.{
  AooApiMarshallerImpl,
  AooApiServiceImpl,
  CategoryApiMarshallerImpl,
  CategoryApiServiceImpl,
  DatasourceApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl,
  UoApiMarshallerImpl,
  UoApiServiceImpl
}
import it.pagopa.interop.partyregistryproxy.api.{AooApi, CategoryApi, DatasourceApi, HealthApi, InstitutionApi, UoApi}
import it.pagopa.interop.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import it.pagopa.interop.partyregistryproxy.service.impl.{
  AooIndexSearchServiceImpl,
  AooIndexWriterServiceImpl,
  CategoryIndexSearchServiceImpl,
  CategoryIndexWriterServiceImpl,
  IPAOpenDataServiceImpl,
  InstitutionIndexSearchServiceImpl,
  InstitutionIndexWriterServiceImpl,
  UoIndexSearchServiceImpl,
  UoIndexWriterServiceImpl
}
import it.pagopa.interop.partyregistryproxy.service.{IndexWriterService, OpenDataService}

import scala.concurrent.ExecutionContext

trait Dependencies {

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  def http()(implicit actorSystem: ActorSystem[_]): HttpExt = Http()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator),
    loggingEnabled = false
  )

  val institutionsWriterService: IndexWriterService[Institution] = InstitutionIndexWriterServiceImpl
  val aooWriterService: IndexWriterService[Institution]          = AooIndexWriterServiceImpl
  val uoWriterService: IndexWriterService[Institution]           = UoIndexWriterServiceImpl
  val categoriesWriterService: IndexWriterService[Category]      = CategoryIndexWriterServiceImpl
  def getOpenDataService()(implicit ec: ExecutionContext, actorSystem: ActorSystem[_]): OpenDataService =
    IPAOpenDataServiceImpl(http())

  def institutionApi()(implicit jwtReader: JWTReader): InstitutionApi = new InstitutionApi(
    InstitutionApiServiceImpl(InstitutionIndexSearchServiceImpl),
    InstitutionApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def aooApi()(implicit jwtReader: JWTReader): AooApi = new AooApi(
    AooApiServiceImpl(AooIndexSearchServiceImpl),
    AooApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def uoApi()(implicit jwtReader: JWTReader): UoApi =
    new UoApi(UoApiServiceImpl(UoIndexSearchServiceImpl), UoApiMarshallerImpl, jwtReader.OAuth2JWTValidatorAsContexts)

  def categoryApi()(implicit jwtReader: JWTReader): CategoryApi = new CategoryApi(
    CategoryApiServiceImpl(CategoryIndexSearchServiceImpl),
    CategoryApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def datasourceApi(
    openDataService: OpenDataService,
    institutionsWriterService: IndexWriterService[Institution],
    aooWriterService: IndexWriterService[Institution],
    uoWriterService: IndexWriterService[Institution],
    categoriesWriterService: IndexWriterService[Category]
  )(blockingEc: ExecutionContext): DatasourceApi = new DatasourceApi(
    new DatasourceApiServiceImpl(
      openDataService,
      institutionsWriterService,
      aooWriterService,
      uoWriterService,
      categoriesWriterService
    )(blockingEc),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator)
  )

  def createJwtReader(keyset: Map[KID, SerializedKey]): JWTReader = new DefaultJWTReader with PublicKeysHolder {
    var publicKeyset: Map[KID, SerializedKey] = keyset

    override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
      getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
  }
}

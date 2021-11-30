package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.{Http, HttpExt}
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl,
  LoaderApiMarshallerImpl,
  LoaderApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.{HealthApi, InstitutionApi, LoaderApi}
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
    val _ = OpenDataLoading.loadOpenData(openDataService, institutionsSearchService, categoriesSearchService)
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(institutionsSearchService, categoriesSearchService),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val loaderApi: LoaderApi =
    new LoaderApi(
      new LoaderApiServiceImpl(openDataService, institutionsSearchService, categoriesSearchService),
      new LoaderApiMarshallerImpl(),
      SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    )

  val initialDelay: Long = getInitialDelay(ApplicationConfiguration.cronTime)

  val controller = new Controller(healthApi, institutionApi)

  logger.error(s"Started build info = ${buildinfo.BuildInfo.toString}")

  val bindingFuture =
    http
      .newServerAt("0.0.0.0", ApplicationConfiguration.serverPort)
      .bind(corsHandler(loaderApi.route ~ controller.routes))

}

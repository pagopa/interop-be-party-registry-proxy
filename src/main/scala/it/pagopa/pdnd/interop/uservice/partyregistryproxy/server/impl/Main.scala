package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  InstitutionApiMarshallerImpl,
  HealthApiServiceImpl,
  HealthApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.HealthApi
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApi
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{Authenticator, classicActorSystem}
import kamon.Kamon

object Main extends App {

  Kamon.init()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller = new Controller(healthApi, institutionApi)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bind(controller.routes)
}

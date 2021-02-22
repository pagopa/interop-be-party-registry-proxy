package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  OrganizationApiMarshallerImpl,
  OrganizationApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.OrganizationApi
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{Authenticator, classicActorSystem}
import kamon.Kamon

object Main extends App {

  Kamon.init()

  val api = new OrganizationApi(
    new OrganizationApiServiceImpl(),
    new OrganizationApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller = new Controller(api)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088, ).bind(controller.routes)
}

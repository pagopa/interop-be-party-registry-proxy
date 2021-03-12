package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  HealthApiMarshallerImpl,
  HealthApiServiceImpl,
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.HealthApi
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApi
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{Authenticator, classicActorSystem}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.LDAPServiceImpl
import kamon.Kamon

import javax.naming.directory.DirContext

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

  val url: Option[String]      = Option(System.getenv("LDAP_URL"))
  val userName: Option[String] = Option(System.getenv("LDAP_USER_NAME"))
  val password: Option[String] = Option(System.getenv("LDAP_PASSWORD"))

  val connection: Option[DirContext] = for {
    url        <- url
    userName   <- userName
    password   <- password
    connection <- LDAPServiceImpl.createConnection(url, userName, password)
  } yield connection

  val ldapService = connection.map(LDAPServiceImpl(_))

  ldapService.foreach(_.getAllInstitutions.foreach(println))

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller = new Controller(healthApi, institutionApi)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bind(controller.routes)
}

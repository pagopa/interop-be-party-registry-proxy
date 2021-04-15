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
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.{HealthApi, InstitutionApi}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{
  Authenticator,
  CorsSupport,
  actorSystem,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.{LDAPService, SearchService}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.{LDAPServiceImpl, SearchServiceImpl}
import kamon.Kamon
import org.slf4j.LoggerFactory

import javax.naming.directory.DirContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object Main extends App with CorsSupport {

  val logger = LoggerFactory.getLogger(this.getClass)

  Kamon.init()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val searchService: SearchService = SearchServiceImpl(config.getString("service-config.index-dir"))

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(searchService),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val connection: Try[DirContext] = LDAPService.createConnection(vault)

  val ldapService: Try[LDAPService] = connection.map(LDAPServiceImpl.create)

  val cronTime = config.getString("service-config.ipa-update-time")

  actorSystem.scheduler.scheduleAtFixedRate(getInitialDelay(cronTime).milliseconds, 24.hours) { () =>
    logger.info("Creating index from iPA")

    val result = for {
      ldap <- ldapService
      _    <- searchService.deleteAll()
      _ = logger.info(s"Institutions deleted")
      _ <- searchService.adds(ldap.getAllInstitutions)
      _ = logger.info(s"Institutions inserted")
    } yield searchService.commit()

    result match {
      case Success(_) => logger.info(s"Institutions committed")
      case Failure(ex) =>
        logger.error(s"Error trying to populate index, due: ${ex.getMessage}")
        ex.printStackTrace()
    }

  }

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller = new Controller(healthApi, institutionApi)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8088).bind(corsHandler(controller.routes))
}

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
  actorSystem,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.{LDAPServiceImpl, SearchServiceImpl}
import kamon.Kamon
import org.slf4j.LoggerFactory
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import javax.naming.directory.DirContext
import scala.concurrent.duration._
import scala.util.{Success, Try, Failure}

object Main extends App {

  val logger = LoggerFactory.getLogger(this.getClass)

  Kamon.init()

  val url: Option[String]           = Option(System.getenv("LDAP_URL"))
  val userName: Option[String]      = Option(System.getenv("LDAP_USER_NAME"))
  val password: Option[String]      = Option(System.getenv("LDAP_PASSWORD"))
  val ipaUpdateTime: Option[String] = Option(System.getenv("IPA_UPDATE_TIME"))
  val indexDir: Option[String]      = Option(System.getenv("INDEX_DIR"))

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val searchService: SearchService = SearchServiceImpl(indexDir.getOrElse("index"))

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(searchService),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val parametersErrorMessage: String =
    s"LDAP connection parameter missed: " +
      s"url:${url.fold("NOT SET")(_ => "SET")}, " +
      s"userName:${userName.fold("NOT SET")(_ => "SET")}, " +
      s"password: ${password.fold("NOT SET")(_ => "SET")}"

  val connection: Try[DirContext] = {
    for {
      url        <- url
      userName   <- userName
      password   <- password
      connection <- LDAPServiceImpl.createConnection(url, userName, password)
    } yield connection
  }
    .toRight(new RuntimeException(parametersErrorMessage))
    .toTry

  val ldapService: Try[LDAPServiceImpl] = connection.map(LDAPServiceImpl.create)

  val cronTime = ipaUpdateTime.getOrElse("22:35")

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

  val bindingFuture = Http().newServerAt("0.0.0.0", 8090).bind(cors()(controller.routes))
}

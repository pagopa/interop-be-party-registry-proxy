package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl

import akka.actor.typed.ActorSystem
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
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.persistence.InstitutionsPersistentBehavior
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.persistence.InstitutionsPersistentBehavior.AddInstitution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.LDAPServiceImpl
import kamon.Kamon

import javax.naming.directory.DirContext
import scala.concurrent.duration.{DurationInt, DurationLong}

object Main extends App {

  Kamon.init()

  val healthApi: HealthApi = new HealthApi(
    new HealthApiServiceImpl(),
    new HealthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val institutionCommander =
    ActorSystem(InstitutionsPersistentBehavior(), "pdnd-interop-uservice-party-registry-proxy")

  val institutionApi: InstitutionApi = new InstitutionApi(
    new InstitutionApiServiceImpl(institutionCommander),
    new InstitutionApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  val url: Option[String]           = Option(System.getenv("LDAP_URL"))
  val userName: Option[String]      = Option(System.getenv("LDAP_USER_NAME"))
  val password: Option[String]      = Option(System.getenv("LDAP_PASSWORD"))
  val ipaUpdateTime: Option[String] = Option(System.getenv("IPA_UPDATE_TIME"))

  val connection: Option[DirContext] = for {
    url        <- url
    userName   <- userName
    password   <- password
    connection <- LDAPServiceImpl.createConnection(url, userName, password)
  } yield connection

  val ldapService: Option[LDAPServiceImpl] = connection.map(c => LDAPServiceImpl(c))

  val cronTime = ipaUpdateTime.getOrElse("22:35")

  actorSystem.scheduler.scheduleAtFixedRate(getInitialDelay(cronTime).milliseconds, 24.hours) { () =>
    println("LOADING FROM IPA")
    ldapService.foreach { service =>
      var count = 0
      service.getAllInstitutions.foreach { institution =>
        count += 1
        institutionCommander.tell(AddInstitution(institution))
      }
      println(count)
    }
  }

//  actorSystem.scheduler.scheduleAtFixedRate(0.milliseconds, 24.hours) { () =>
//    println("LOADING FROM IPA")
//    ldapService.foreach { service =>
//      var count = 0
//      service.getAllInstitutions.foreach { institution =>
//        count += 1
//        institutionCommander.tell(AddInstitution(institution))
//      }
//      println(count)
//    }
//  }

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller = new Controller(healthApi, institutionApi)

  val bindingFuture = Http().newServerAt("0.0.0.0", 8090).bind(controller.routes)
}

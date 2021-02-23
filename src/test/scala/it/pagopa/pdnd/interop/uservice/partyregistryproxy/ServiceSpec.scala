package it.pagopa.pdnd.interop.uservice.partyregistryproxy

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.OrganizationApiMarshallerImpl
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.{
  OrganizationApi,
  OrganizationApiMarshaller,
  OrganizationApiService
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Organization, OrganizationError}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol.{jsonFormat4, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.OptionPartial"
  )
)
class ServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  import ServiceSpecSupport._

  val organizationMarshaller: OrganizationApiMarshaller = new OrganizationApiMarshallerImpl

  val url: String = "http://localhost:8088/pdnd-interop-uservice-party-registry-proxy/0.0.1/organizations"

  import organizationMarshaller._

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val organizationApiService: OrganizationApiService = mock[OrganizationApiService]

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit] = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    val api: OrganizationApi                = new OrganizationApi(organizationApiService, organizationMarshaller, wrappingDirective)

    controller = Some(new Controller(api))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 8088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100 seconds)
    }

  }

  override def afterAll(): Unit = {

    bindServer.foreach(_.foreach(_.unbind()))

  }

  "Asking for an organization" must {
    "work successfully" in {

      implicit val unmarshaller: FromEntityUnmarshaller[Organization] =
        sprayJsonUnmarshaller(jsonFormat4(Organization.apply))
      (
        organizationApiService
          .getOrganizationById(_: String)(_: ToEntityMarshaller[Organization], _: ToEntityMarshaller[OrganizationError])
        )
        .expects(validOrdId, *, *)
        .returning(complete(StatusCodes.OK, responseOk))

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$validOrdId", method = HttpMethods.GET))
          .flatMap { response =>
            Unmarshal(response.entity).to[Organization]
          },
        Duration.Inf
      )
      body must be(responseOk)

    }

    "return 404 for an organization not found" in {
      implicit val unmarshaller: FromEntityUnmarshaller[OrganizationError] =
        sprayJsonUnmarshaller(jsonFormat4(OrganizationError.apply))
      (
        organizationApiService
          .getOrganizationById(_: String)(_: ToEntityMarshaller[Organization], _: ToEntityMarshaller[OrganizationError])
        )
        .expects(notFoundOrdId, *, *)
        .returning(complete(StatusCodes.NotFound, responseNotFound))
      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$notFoundOrdId", method = HttpMethods.GET))
          .flatMap { response =>
            Unmarshal(response.entity).to[OrganizationError].map((response.status, _))
          },
        Duration.Inf
      )

      body must be((StatusCodes.NotFound, responseNotFound))

    }
    "return 400 fo an invalid request" in {
      implicit val unmarshaller: FromEntityUnmarshaller[OrganizationError] =
        sprayJsonUnmarshaller(jsonFormat4(OrganizationError.apply))
      (
        organizationApiService
          .getOrganizationById(_: String)(_: ToEntityMarshaller[Organization], _: ToEntityMarshaller[OrganizationError])
        )
        .expects(invalidOrdId, *, *)
        .returning(complete(StatusCodes.BadRequest, responseInvalid))
      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$invalidOrdId", method = HttpMethods.GET))
          .flatMap(response => Unmarshal(response.entity).to[OrganizationError].map((response.status, _))),
        Duration.Inf
      )

      body must be((StatusCodes.BadRequest, responseInvalid))

    }

  }

}

object ServiceSpecSupport {

  final lazy val validOrdId    = "id1"
  final lazy val notFoundOrdId = "id2"
  final lazy val invalidOrdId  = "id3"

  final lazy val responseOk       = Organization("id", "dn", "description", "pec")
  final lazy val responseNotFound = OrganizationError("not found", "Bad request", "Error", "404")
  final lazy val responseInvalid  = OrganizationError("invalid id", "Bad request", "Error", "400")
}

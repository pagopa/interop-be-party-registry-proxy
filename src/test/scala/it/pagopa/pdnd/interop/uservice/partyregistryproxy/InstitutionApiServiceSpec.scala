package it.pagopa.pdnd.interop.uservice.partyregistryproxy

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.{
  HealthApi,
  InstitutionApi,
  InstitutionApiMarshaller,
  InstitutionApiService
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol.{jsonFormat4, _}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.Null"
  )
)
class InstitutionApiServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  import ServiceSpecSupport._

  val institutionApiMarshaller: InstitutionApiMarshaller = new InstitutionApiMarshallerImpl

  val url: String = "http://localhost:8088/pdnd-interop-uservice-party-registry-proxy/0.0.1/institutions"

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit]          = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    val institutionApiService: InstitutionApiService = new InstitutionApiServiceImpl()
    val institutionApi: InstitutionApi =
      new InstitutionApi(institutionApiService, institutionApiMarshaller, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(new Controller(healthApi, institutionApi))

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

  "Asking for an institutions" must {
    "work successfully" in {

      implicit val unmarshaller: FromEntityUnmarshaller[Institution] =
        sprayJsonUnmarshaller(jsonFormat4(Institution.apply))

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$validOrdId", method = HttpMethods.GET))
          .flatMap { response =>
            Unmarshal(response.entity).to[Institution]
          },
        Duration.Inf
      )
      body must be(responseOk)

    }

    "return 404 for an organization not found" in {
      implicit val unmarshaller: FromEntityUnmarshaller[ErrorResponse] =
        sprayJsonUnmarshaller(jsonFormat4(ErrorResponse.apply))

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$notFoundOrdId", method = HttpMethods.GET))
          .flatMap { response =>
            Unmarshal(response.entity).to[ErrorResponse].map((response.status, _))
          },
        Duration.Inf
      )

      body must be((StatusCodes.NotFound, responseNotFound))

    }
    "return 400 fo an invalid request" in {
      implicit val unmarshaller: FromEntityUnmarshaller[ErrorResponse] =
        sprayJsonUnmarshaller(jsonFormat4(ErrorResponse.apply))

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url/$invalidOrdId", method = HttpMethods.GET))
          .flatMap(response => Unmarshal(response.entity).to[ErrorResponse].map((response.status, _))),
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

  final lazy val responseOk       = Institution("id", "dn", "description", "pec")
  final lazy val responseNotFound = ErrorResponse("not found", "Bad request", "Error", "404")
  final lazy val responseInvalid  = ErrorResponse("invalid id", "Bad request", "Error", "400")
}

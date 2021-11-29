package it.pagopa.pdnd.interop.uservice.partyregistryproxy

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl.{
  InstitutionApiMarshallerImpl,
  InstitutionApiServiceImpl,
  _
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
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.Controller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.SearchService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.InstitutionFields
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class InstitutionApiServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  import ServiceSpecSupport._

  System.setProperty(
    "PARTY_REGISTRY_CATEGORIES_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/84ebb2e7-0e61-427b-a1dd-ab8bb2a84f07?format=json"
  )
  System.setProperty(
    "PARTY_REGISTRY_INSTITUTIONS_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/d09adf99-dc10-4349-8c53-27b1e5aa97b6?format=json"
  )

  val institutionApiMarshaller: InstitutionApiMarshaller = new InstitutionApiMarshallerImpl

  val url: String =
    s"http://localhost:8088/pdnd-interop-uservice-party-registry-proxy/${buildinfo.BuildInfo.interfaceVersion}/institutions"

  var controller: Option[Controller]                       = None
  var bindServer: Option[Future[Http.ServerBinding]]       = None
  val institutionSearchService: SearchService[Institution] = mock[SearchService[Institution]]
  val categorySearchService: SearchService[Category]       = mock[SearchService[Category]]

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit] = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)

    val institutionApiService: InstitutionApiService =
      new InstitutionApiServiceImpl(institutionSearchService, categorySearchService)
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
    "work successfully for page = 1 and limit = 1" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 1

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(InstitutionFields.DESCRIPTION, searchTxt, page, limit)
        .returning(Success(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response = Await.result(
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url?search=$searchTxt&page=${page.toString}&limit=${limit.toString}",
              method = HttpMethods.GET
            )
          ),
        Duration.Inf
      )

      val body     = Await.result(Unmarshal(response.entity).to[Institutions], Duration.Inf)
      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9211")
      body must be(Institutions(expected, expected.size.toLong))
      response.status must be(StatusCodes.OK)

    }

    "work successfully for page = 2 and limit = 1" in {

      val searchTxt = "Institution"
      val page      = 2
      val limit     = 1

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Success(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response = Await.result(
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url?search=$searchTxt&page=${page.toString}&limit=${limit.toString}",
              method = HttpMethods.GET
            )
          ),
        Duration.Inf
      )

      val body     = Await.result(Unmarshal(response.entity).to[Institutions], Duration.Inf)
      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9212")
      body must be(Institutions(expected, expected.size.toLong))
      response.status must be(StatusCodes.OK)

    }

    "work successfully for page = 1 and limit = 4" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 4

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Success(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response = Await.result(
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url?search=$searchTxt&page=${page.toString}&limit=${limit.toString}",
              method = HttpMethods.GET
            )
          ),
        Duration.Inf
      )

      val body     = Await.result(Unmarshal(response.entity).to[Institutions], Duration.Inf)
      val expected = institutions
      body must be(Institutions(expected, expected.size.toLong))
      response.status must be(StatusCodes.OK)

    }

    "work successfully for page = 2 and limit = 2" in {

      val searchTxt = "Institution"
      val page      = 2
      val limit     = 2

      val searchResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page, page + limit)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Success(searchResponse -> searchResponse.size.toLong))
        .once()

      val response = Await.result(
        Http()
          .singleRequest(
            HttpRequest(
              uri = s"$url?search=$searchTxt&page=${page.toString}&limit=${limit.toString}",
              method = HttpMethods.GET
            )
          ),
        Duration.Inf
      )

      val body = Await.result(Unmarshal(response.entity).to[Institutions], Duration.Inf)
      val expected = institutions.filter(i =>
        Set("27f8dce0-0a5b-476b-9fdd-a7a658eb9213", "27f8dce0-0a5b-476b-9fdd-a7a658eb9214").contains(i.id)
      )
      body must be(Institutions(expected, expected.size.toLong))
      response.status must be(StatusCodes.OK)

    }

    "return 404 for an organization not found" in {

      val searchTxt = "Organization"

      val searchResponse = institutions.filter(_.description.contains(searchTxt))

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, *, *)
        .returning(Success(searchResponse -> searchResponse.size.toLong))
        .once()

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url?search=$searchTxt&page=1&limit=1", method = HttpMethods.GET))
          .flatMap { response =>
            Unmarshal(response.entity).to[Problem].map((response.status, _))
          },
        Duration.Inf
      )

      body must be((StatusCodes.NotFound, responseNotFound))

    }
    "return 400 for an invalid request" in {

      (institutionSearchService.searchByText _)
        .expects(*, *, *, *)
        .returning(Failure(new RuntimeException("Something goes wrong")))
        .once()

      val body = Await.result(
        Http()
          .singleRequest(HttpRequest(uri = s"$url?search=text&page=1&limit=1", method = HttpMethods.GET))
          .flatMap(response => Unmarshal(response.entity).to[Problem].map((response.status, _))),
        Duration.Inf
      )

      body must be((StatusCodes.BadRequest, responseInvalid))

    }

  }

}

object ServiceSpecSupport {

  final lazy val institutionOne = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode1",
    category = "cat1",
    manager = Manager("name", "surname"),
    description = "Institution One",
    digitalAddress = "digitalAddress1"
  )

  final lazy val institutionTwo = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9212",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode2",
    category = "cat2",
    manager = Manager("name", "surname"),
    description = "Institution Two",
    digitalAddress = "digitalAddress2"
  )

  final lazy val institutionThree = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9213",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode3",
    category = "cat3",
    manager = Manager("name", "surname"),
    description = "Institution Three",
    digitalAddress = "digitalAddress3"
  )

  final lazy val institutionFour = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9214",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode4",
    category = "cat4",
    manager = Manager("name", "surname"),
    description = "Institution Four",
    digitalAddress = "digitalAddress4"
  )

  final lazy val institutions = List(institutionOne, institutionTwo, institutionThree, institutionFour)

  final lazy val responseNotFound = Problem(None, 404, "Not found")
  final lazy val responseInvalid  = Problem(Some("Something goes wrong"), 400, "Invalid")
}

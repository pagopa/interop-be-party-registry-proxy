package it.pagopa.interop.partyregistryproxy

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.SecurityDirectives
import it.pagopa.interop.partyregistryproxy.api._
import it.pagopa.interop.partyregistryproxy.api.impl.{CategoryApiMarshallerImpl, _}
import it.pagopa.interop.partyregistryproxy.common.system.{Authenticator, classicActorSystem, executionContext}
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.common.util.createCategoryId
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors.{
  CategoriesNotFound,
  CategoryNotFound,
  InstitutionsNotFound,
  InvalidSearchInstitutionRequest
}
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.server.Controller
import it.pagopa.interop.partyregistryproxy.service.IndexSearchService
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class PartyRegistryProxySpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  import ServiceSpecSupport._

  System.setProperty(
    "PARTY_REGISTRY_CATEGORIES_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/84ebb2e7-0e61-427b-a1dd-ab8bb2a84f07?format=json"
  )
  System.setProperty(
    "PARTY_REGISTRY_INSTITUTIONS_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/d09adf99-dc10-4349-8c53-27b1e5aa97b6?format=json"
  )

  val institutionApiMarshaller: InstitutionApiMarshaller = InstitutionApiMarshallerImpl
  val categoryApiMarshaller: CategoryApiMarshaller       = CategoryApiMarshallerImpl

  var controller: Option[Controller]                            = None
  var bindServer: Option[Future[Http.ServerBinding]]            = None
  val institutionSearchService: IndexSearchService[Institution] = mock[IndexSearchService[Institution]]
  val categorySearchService: IndexSearchService[Category]       = mock[IndexSearchService[Category]]

  override def beforeAll(): Unit = {

    val wrappingDirective: Directive1[Unit] = SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)

    val institutionApiService: InstitutionApiService = InstitutionApiServiceImpl(institutionSearchService)
    val institutionApi: InstitutionApi =
      new InstitutionApi(institutionApiService, institutionApiMarshaller, wrappingDirective)

    val categoryApiService: CategoryApiService = CategoryApiServiceImpl(categorySearchService)

    val categoryApi: CategoryApi =
      new CategoryApi(categoryApiService, categoryApiMarshaller, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(new Controller(health = healthApi, institution = institutionApi, category = categoryApi))

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

  "Asking for institutions" must {
    "work successfully for page = 1 and limit = 1" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 1

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(DESCRIPTION.value, searchTxt, page, limit)
        .returning(Success(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9211")

      response.status must be(StatusCodes.OK)
      response.body must be(Institutions(expected, expected.size.toLong))

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

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9212")

      response.status must be(StatusCodes.OK)
      response.body must be(Institutions(expected, expected.size.toLong))

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

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions

      response.status must be(StatusCodes.OK)
      response.body must be(Institutions(expected, expected.size.toLong))

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

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")
      val expected = institutions.filter(i =>
        Set("27f8dce0-0a5b-476b-9fdd-a7a658eb9213", "27f8dce0-0a5b-476b-9fdd-a7a658eb9214").contains(i.id)
      )
      response.body must be(Institutions(expected, expected.size.toLong))
      response.status must be(StatusCodes.OK)

    }

    "return 404 for an organization not found" in {

      val searchTxt = "Organization"

      val searchResponse = institutions.filter(_.description.contains(searchTxt))

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, *, *)
        .returning(Success(searchResponse -> searchResponse.size.toLong))
        .once()

      val body = makeRequest[Problem](s"institutions?search=$searchTxt&page=1&limit=1")

      body must be(SpecResult(StatusCodes.NotFound, responseInstitutionsNotFound))

    }
    "return 400 for an invalid request" in {

      (institutionSearchService.searchByText _)
        .expects(*, *, *, *)
        .returning(Failure(new RuntimeException("Something goes wrong")))
        .once()

      val body = makeRequest[Problem](s"institutions?search=text&page=1&limit=1")

      body must be(SpecResult(StatusCodes.BadRequest, responseInvalidSearch))

    }

  }

  "Asking for categories" must {
    "retrieve all categories" in {

      (categorySearchService.getAllItems _)
        .expects(*)
        .returning(Success(categories))
        .once()

      val response = makeRequest[Categories](s"categories")

      response must be(SpecResult(StatusCodes.OK, Categories(categories)))

    }

    "retrieve all categories specifying origin" in {

      (categorySearchService.getAllItems _)
        .expects(*)
        .returning(Success(categories.filter(_.origin == originOne)))
        .once()

      val response = makeRequest[Categories](s"categories?origin=$originOne")

      response must be(SpecResult(StatusCodes.OK, Categories(categories.filter(_.origin == originOne))))

    }

    "return 404 if the specified origin does not exist" in {

      (categorySearchService.getAllItems _)
        .expects(*)
        .returning(Success(List.empty))
        .once()

      val response = makeRequest[Problem](s"categories?origin=$originThree")

      response must be(SpecResult(StatusCodes.NotFound, responseCategoriesNotFound))

    }

    "retrieve a category" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originOne, categoryCodeOne))
        .returning(Success(Some(categoryOne)))
        .once()

      val response = makeRequest[Category](s"origins/$originOne/categories/$categoryCodeOne")

      response must be(SpecResult(StatusCodes.OK, categoryOne))

    }

    "return 404 if the category does not exist" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originOne, categoryCodeFour))
        .returning(Success(categories.find(_.code == categoryCodeFour)))
        .once()

      val response = makeRequest[Problem](s"origins/$originOne/categories/$categoryCodeFour")

      response must be(SpecResult(StatusCodes.NotFound, responseCategoryNotFound(categoryCodeFour)))

    }

    "return 404 the origin does not exist" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originThree, categoryCodeOne))
        .returning(Success(categories.find(_.origin == originThree)))
        .once()

      val response = makeRequest[Problem](s"origins/$originThree/categories/$categoryCodeOne")

      response must be(SpecResult(StatusCodes.NotFound, responseCategoryNotFound(categoryCodeOne)))

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
    description = "Institution One",
    digitalAddress = "digitalAddress1",
    address = "address1",
    zipCode = "zipCode1",
    origin = "origin"
  )

  final lazy val institutionTwo = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9212",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode2",
    category = "cat2",
    description = "Institution Two",
    digitalAddress = "digitalAddress2",
    address = "address2",
    zipCode = "zipCode2",
    origin = "origin"
  )

  final lazy val institutionThree = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9213",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode3",
    category = "cat3",
    description = "Institution Three",
    digitalAddress = "digitalAddress3",
    address = "address3",
    zipCode = "zipCode3",
    origin = "origin"
  )

  final lazy val institutionFour = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9214",
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode4",
    category = "cat4",
    description = "Institution Four",
    digitalAddress = "digitalAddress4",
    address = "address4",
    zipCode = "zipCode4",
    origin = "origin"
  )

  final lazy val institutions = List(institutionOne, institutionTwo, institutionThree, institutionFour)

  val originOne   = "origin1"
  val originTwo   = "origin2"
  val originThree = "origin3"

  val categoryCodeOne   = "code1"
  val categoryCodeTwo   = "code2"
  val categoryCodeThree = "code3"
  val categoryCodeFour  = "code4"

  val categoryOne: Category    = Category(code = categoryCodeOne, name = "name1", kind = "kind1", origin = originOne)
  val categoryTwo: Category    = Category(code = categoryCodeTwo, name = "name2", kind = "kind2", origin = originOne)
  val categoryOThree: Category = Category(code = categoryCodeThree, name = "name3", kind = "kind3", origin = originTwo)

  final lazy val categories = List(categoryOne, categoryTwo, categoryOThree)

  final lazy val responseInstitutionsNotFound         = problemOf(StatusCodes.NotFound, InstitutionsNotFound)
  final lazy val responseInvalidSearch                = problemOf(StatusCodes.BadRequest, InvalidSearchInstitutionRequest)
  final lazy val responseCategoriesNotFound           = problemOf(StatusCodes.NotFound, CategoriesNotFound)
  def responseCategoryNotFound(code: String): Problem = problemOf(StatusCodes.NotFound, CategoryNotFound(code))
}

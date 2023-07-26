package it.pagopa.interop.partyregistryproxy

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import it.pagopa.interop.commons.utils.AkkaUtils.Authenticator
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.errors.{Problem => CommonProblem}
import it.pagopa.interop.partyregistryproxy.api._
import it.pagopa.interop.partyregistryproxy.api.impl._
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField.DESCRIPTION
import it.pagopa.interop.partyregistryproxy.common.util.createCategoryId
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors.CategoryNotFound
import it.pagopa.interop.partyregistryproxy.model._
import it.pagopa.interop.partyregistryproxy.server.Controller
import it.pagopa.interop.partyregistryproxy.service.{IndexSearchService, IndexWriterService, OpenDataService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class PartyRegistryProxySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with MockFactory {

  import ServiceSpecSupport._

  System.setProperty(
    "PARTY_REGISTRY_CATEGORIES_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/84ebb2e7-0e61-427b-a1dd-ab8bb2a84f07?format=json"
  )
  System.setProperty(
    "PARTY_REGISTRY_INSTITUTIONS_URL",
    "https://indicepa.gov.it/ipa-dati/datastore/dump/d09adf99-dc10-4349-8c53-27b1e5aa97b6?format=json"
  )

  implicit val ec: ExecutionContext       = implicitly[ActorSystem[_]].executionContext
  implicit val as: akka.actor.ActorSystem = implicitly[ActorSystem[_]].classicSystem

  val institutionApiMarshaller: InstitutionApiMarshaller = InstitutionApiMarshallerImpl
  val aooApiMarshaller: AooApiMarshaller                 = AooApiMarshallerImpl
  val uoApiMarshaller: UoApiMarshaller                   = UoApiMarshallerImpl
  val categoryApiMarshaller: CategoryApiMarshaller       = CategoryApiMarshallerImpl

  var controller: Option[Controller]                            = None
  var bindServer: Option[Future[Http.ServerBinding]]            = None
  val institutionSearchService: IndexSearchService[Institution] = mock[IndexSearchService[Institution]]
  val aooSearchService: IndexSearchService[Institution]         = mock[IndexSearchService[Institution]]
  val uoSearchService: IndexSearchService[Institution]          = mock[IndexSearchService[Institution]]
  val categorySearchService: IndexSearchService[Category]       = mock[IndexSearchService[Category]]
  val institutionWriterService: IndexWriterService[Institution] = mock[IndexWriterService[Institution]]
  val aooWriterService: IndexWriterService[Institution]         = mock[IndexWriterService[Institution]]
  val uoWriterService: IndexWriterService[Institution]          = mock[IndexWriterService[Institution]]
  val categoryWriterService: IndexWriterService[Category]       = mock[IndexWriterService[Category]]

  val openDataService: OpenDataService = mock[OpenDataService]

  override def beforeAll(): Unit = {

    val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
      SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)

    val institutionApiService: InstitutionApiService = InstitutionApiServiceImpl(institutionSearchService)
    val institutionApi: InstitutionApi               =
      new InstitutionApi(institutionApiService, institutionApiMarshaller, wrappingDirective)

    val aooApiService: AooApiService = AooApiServiceImpl(aooSearchService)
    val aooApi: AooApi               = new AooApi(aooApiService, aooApiMarshaller, wrappingDirective)

    val uoApiService: UoApiService = UoApiServiceImpl(uoSearchService)
    val uoApi: UoApi               = new UoApi(uoApiService, uoApiMarshaller, wrappingDirective)

    val categoryApiService: CategoryApiService = CategoryApiServiceImpl(categorySearchService)

    val categoryApi: CategoryApi =
      new CategoryApi(categoryApiService, categoryApiMarshaller, wrappingDirective)

    val datasourceApiService: DatasourceApiService =
      new DatasourceApiServiceImpl(
        openDataService,
        institutionWriterService,
        aooWriterService,
        uoWriterService,
        categoryWriterService
      )(ec)
    val datasourceApi: DatasourceApi               =
      new DatasourceApi(datasourceApiService, wrappingDirective)

    val healthApi: HealthApi = mock[HealthApi]

    controller = Some(
      new Controller(
        health = healthApi,
        institution = institutionApi,
        aoo = aooApi,
        uo = uoApi,
        category = categoryApi,
        datasource = datasourceApi,
        validationExceptionToRoute = Some(report => {
          val error =
            CommonProblem(
              StatusCodes.BadRequest,
              OpenapiUtils.errorFromRequestValidationReport(report),
              serviceCode,
              Some("test-id")
            )
          complete(error.status, error)
        })
      )
    )

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

  "Asking for institutions" should {

    "work Rightfully searching for Institution text using default value (page=1,limit=10)" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 10

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(DESCRIPTION.value, searchTxt, page, limit)
        .returning(Right(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt")

      response.status should be(StatusCodes.OK)
      response.body should be(Institutions(institutions, institutions.size.toLong))

    }

    "work Rightfully searching for Institution text with page = 1 and limit = 1" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 1

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(DESCRIPTION.value, searchTxt, page, limit)
        .returning(Right(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9211")

      response.status should be(StatusCodes.OK)
      response.body should be(Institutions(expected, expected.size.toLong))

    }

    "work Rightfully searching for Institution text with page = 2 and limit = 1" in {

      val searchTxt = "Institution"
      val page      = 2
      val limit     = 1

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Right(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions.filter(_.id == "27f8dce0-0a5b-476b-9fdd-a7a658eb9212")

      response.status should be(StatusCodes.OK)
      response.body should be(Institutions(expected, expected.size.toLong))

    }

    "work Rightfully searching for Institution text with page = 1 and limit = 4" in {

      val searchTxt = "Institution"
      val page      = 1
      val limit     = 4

      val luceneResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page - 1, page + limit - 1)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Right(luceneResponse -> luceneResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      val expected = institutions

      response.status should be(StatusCodes.OK)
      response.body should be(Institutions(expected, expected.size.toLong))

    }

    "work Rightfully searching for Institution text with page = 2 and limit = 2" in {

      val searchTxt = "Institution"
      val page      = 2
      val limit     = 2

      val searchResponse =
        institutions.filter(_.description.contains(searchTxt)).sortBy(_.id).slice(page, page + limit)

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, page, limit)
        .returning(Right(searchResponse -> searchResponse.size.toLong))
        .once()

      val response =
        makeRequest[Institutions](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")
      val expected = institutions.filter(i =>
        Set("27f8dce0-0a5b-476b-9fdd-a7a658eb9213", "27f8dce0-0a5b-476b-9fdd-a7a658eb9214").contains(i.id)
      )
      response.body should be(Institutions(expected, expected.size.toLong))
      response.status should be(StatusCodes.OK)

    }

    "return an empty result if the specified search text does not match with any institution's records." in {

      val searchTxt = "Organization"

      val searchResponse = institutions.filter(_.description.contains(searchTxt))

      (institutionSearchService.searchByText _)
        .expects(*, searchTxt, *, *)
        .returning(Right(searchResponse -> searchResponse.size.toLong))
        .once()

      val body = makeRequest[Institutions](s"institutions?search=$searchTxt&page=1&limit=1")

      body should be(SpecResult(StatusCodes.OK, Institutions(List.empty, 0L)))

    }

    "fail using with page < 1" in {

      val searchTxt = "Institution"
      val page      = 0

      val response = makeRequest[Problem](s"institutions?search=$searchTxt&page=${page.toString}")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.head.detail shouldBe "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"

    }

    "fail using with limit < 1" in {

      val searchTxt = "Institution"
      val limit     = 0

      val response = makeRequest[Problem](s"institutions?search=$searchTxt&limit=${limit.toString}")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.head.detail shouldBe "limit is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"

    }

    "fail using with limit > 1000" in {

      val searchTxt = "Institution"
      val limit     = 1001

      val response = makeRequest[Problem](s"institutions?search=$searchTxt&limit=${limit.toString}")

      response.status shouldBe StatusCodes.BadRequest

      response.body.errors.head.detail shouldBe "limit is not valid - Numeric instance is greater than the required maximum (maximum: 1000, found: 1001)"

    }

    "fail using with page < 1 and limit < 1" in {

      val searchTxt = "Institution"
      val page      = 0
      val limit     = 0

      val response =
        makeRequest[Problem](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain allOf (
        "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)",
        "limit is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"
      )

    }

    "fail using with page < 1 and limit > 1000" in {

      val searchTxt = "Institution"
      val page      = 0
      val limit     = 1001

      val response =
        makeRequest[Problem](s"institutions?search=$searchTxt&page=${page.toString}&limit=${limit.toString}")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain allOf (
        "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)",
        "limit is not valid - Numeric instance is greater than the required maximum (maximum: 1000, found: 1001)"
      )

    }

  }

  "Asking for categories" should {
    "retrieve all categories" in {

      (categorySearchService.getAllItems _)
        .expects(*, *, *)
        .returning(Right(categories -> categories.size.toLong))
        .once()

      val response = makeRequest[Categories](s"categories")

      response should be(SpecResult(StatusCodes.OK, Categories(categories, categories.size.toLong)))

    }

    "retrieve all categories with page = 1 and limit = 1" in {

      val page  = 1
      val limit = 1

      (categorySearchService.getAllItems _)
        .expects(*, page, limit)
        .returning(
          Right(
            categories.slice(page - 1, page + limit - 1) -> categories.slice(page - 1, page + limit - 1).size.toLong
          )
        )
        .once()

      val response = makeRequest[Categories](s"categories?page=$page&limit=$limit")

      response should be(
        SpecResult(
          StatusCodes.OK,
          Categories(
            categories.slice(page - 1, page + limit - 1),
            categories.slice(page - 1, page + limit - 1).size.toLong
          )
        )
      )

    }

    "retrieve all categories with page = 2 and limit = 1" in {

      val page  = 2
      val limit = 1

      (categorySearchService.getAllItems _)
        .expects(*, page, limit)
        .returning(
          Right(
            categories.slice(page - 1, page + limit - 1) -> categories.slice(page - 1, page + limit - 1).size.toLong
          )
        )
        .once()

      val response = makeRequest[Categories](s"categories?page=$page&limit=$limit")

      response should be(
        SpecResult(
          StatusCodes.OK,
          Categories(
            categories.slice(page - 1, page + limit - 1),
            categories.slice(page - 1, page + limit - 1).size.toLong
          )
        )
      )

    }

    "retrieve all categories with page = 1 and limit = 100" in {

      val page  = 1
      val limit = 100

      (categorySearchService.getAllItems _)
        .expects(*, page, limit)
        .returning(Right(categories -> categories.size.toLong))
        .once()

      val response = makeRequest[Categories](s"categories?page=$page&limit=$limit")

      response should be(
        SpecResult(
          StatusCodes.OK,
          Categories(
            categories.slice(page - 1, page + limit - 1),
            categories.slice(page - 1, page + limit - 1).size.toLong
          )
        )
      )

    }

    "retrieve all categories specifying origin" in {

      (categorySearchService.getAllItems _)
        .expects(*, *, *)
        .returning(Right(categories.filter(_.origin == originOne) -> categories.count(_.origin == originOne).toLong))
        .once()

      val response = makeRequest[Categories](s"categories?origin=$originOne")

      response should be(
        SpecResult(
          StatusCodes.OK,
          Categories(categories.filter(_.origin == originOne), categories.count(_.origin == originOne).toLong)
        )
      )

    }

    "return an empty result if the specified origin does not exist" in {

      (categorySearchService.getAllItems _)
        .expects(*, *, *)
        .returning(Right(List.empty -> 0))
        .once()

      val response = makeRequest[Categories](s"categories?origin=$originThree")

      response should be(SpecResult(StatusCodes.OK, Categories(List.empty, 0L)))

    }

    "fail using with page < 1" in {

      val page     = 0
      val response = makeRequest[Problem](s"categories?origin=$originThree&page=$page")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain(
        "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"
      )

    }

    "fail using with limit < 1" in {

      val limit    = 0
      val response = makeRequest[Problem](s"categories?origin=$originThree&limit=$limit")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain(
        "limit is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"
      )

    }

    "fail using with limit > 100" in {

      val limit    = 101
      val response = makeRequest[Problem](s"categories?origin=$originThree&limit=$limit")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain(
        "limit is not valid - Numeric instance is greater than the required maximum (maximum: 100, found: 101)"
      )

    }

    "fail using with page < 1 and limit < 1" in {

      val page     = 0
      val limit    = 0
      val response = makeRequest[Problem](s"categories?origin=$originThree&page=$page&limit=$limit")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain allOf (
        "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)",
        "limit is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)"
      )
    }

    "fail using with page < 1 and limit > 100" in {

      val page     = 0
      val limit    = 101
      val response = makeRequest[Problem](s"categories?origin=$originThree&page=$page&limit=$limit")

      response.status shouldBe StatusCodes.BadRequest
      response.body.errors.map(_.detail) should contain allOf (
        "page is not valid - Numeric instance is lower than the required minimum (minimum: 1, found: 0)",
        "limit is not valid - Numeric instance is greater than the required maximum (maximum: 100, found: 101)"
      )

    }

    "retrieve a category" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originOne, categoryCodeOne))
        .returning(Right(Some(categoryOne)))
        .once()

      val response = makeRequest[Category](s"origins/$originOne/categories/$categoryCodeOne")

      response should be(SpecResult(StatusCodes.OK, categoryOne))

    }

    "return 404 if the category does not exist" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originOne, categoryCodeFour))
        .returning(Right(categories.find(_.code == categoryCodeFour)))
        .once()

      val response = makeRequest[CommonProblem](s"origins/$originOne/categories/$categoryCodeFour")

      response should be(SpecResult(StatusCodes.NotFound, responseCategoryNotFound(categoryCodeFour)))

    }

    "return 404 the origin does not exist" in {

      (categorySearchService.searchById _)
        .expects(createCategoryId(originThree, categoryCodeOne))
        .returning(Right(categories.find(_.origin == originThree)))
        .once()

      val response = makeRequest[CommonProblem](s"origins/$originThree/categories/$categoryCodeOne")

      response should be(SpecResult(StatusCodes.NotFound, responseCategoryNotFound(categoryCodeOne)))

    }

  }

}

object ServiceSpecSupport {

  final lazy val institutionOne = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9211",
    originId = "originId1",
    taxCode = "taxCode1",
    category = "cat1",
    description = "Institution One",
    digitalAddress = "digitalAddress1",
    address = "address1",
    zipCode = "zipCode1",
    origin = "origin",
    kind = "Pubbliche Amministrazioni",
    classification = Classification.AGENCY
  )

  final lazy val institutionTwo = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9212",
    originId = "originId2",
    taxCode = "taxCode2",
    category = "cat2",
    description = "Institution Two",
    digitalAddress = "digitalAddress2",
    address = "address2",
    zipCode = "zipCode2",
    origin = "origin",
    kind = "Pubbliche Amministrazioni",
    classification = Classification.AGENCY
  )

  final lazy val institutionThree = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9213",
    originId = "originId3",
    taxCode = "taxCode3",
    category = "cat3",
    description = "Institution Three",
    digitalAddress = "digitalAddress3",
    address = "address3",
    zipCode = "zipCode3",
    origin = "origin",
    kind = "Pubbliche Amministrazioni",
    classification = Classification.AGENCY
  )

  final lazy val institutionFour = Institution(
    id = "27f8dce0-0a5b-476b-9fdd-a7a658eb9214",
    originId = "originId4",
    taxCode = "taxCode4",
    category = "cat4",
    description = "Institution Four",
    digitalAddress = "digitalAddress4",
    address = "address4",
    zipCode = "zipCode4",
    origin = "origin",
    kind = "Pubbliche Amministrazioni",
    classification = Classification.AOO
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

  def responseCategoryNotFound(code: String): CommonProblem =
    CommonProblem(StatusCodes.NotFound, CategoryNotFound(code), serviceCode, Some("test-id"))
}

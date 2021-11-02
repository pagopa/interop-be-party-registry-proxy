package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Categories
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institutions
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Problem

class InstitutionApi(
  institutionService: InstitutionApiService,
  institutionMarshaller: InstitutionApiMarshaller,
  wrappingDirective: Directive1[Unit]
) {

  import institutionMarshaller._

  lazy val route: Route =
    path("catergories") {
      get {
        wrappingDirective { _ =>
          institutionService.getCategories()
        }
      }
    } ~
      path("institutions" / Segment) { (institutionId) =>
        get {
          wrappingDirective { _ =>
            institutionService.getInstitutionById(institutionId = institutionId)
          }
        }
      } ~
      path("institutions") {
        get {
          wrappingDirective { _ =>
            parameters("search".as[String], "page".as[Int], "limit".as[Int]) { (search, page, limit) =>
              institutionService.searchInstitution(search = search, page = page, limit = limit)
            }
          }
        }
      }
}

trait InstitutionApiService {
  def getCategories200(responseCategories: Categories)(implicit
    toEntityMarshallerCategories: ToEntityMarshaller[Categories]
  ): Route =
    complete((200, responseCategories))

  /** Code: 200, Message: successful operation, DataType: Categories
    */
  def getCategories()(implicit toEntityMarshallerCategories: ToEntityMarshaller[Categories]): Route

  def getInstitutionById200(responseInstitution: Institution)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution]
  ): Route =
    complete((200, responseInstitution))
  def getInstitutionById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def getInstitutionById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Institution
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitution: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def searchInstitution200(responseInstitutions: Institutions)(implicit
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route =
    complete((200, responseInstitutions))
  def searchInstitution400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def searchInstitution404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Institutions
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  def searchInstitution(search: String, page: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route

}

trait InstitutionApiMarshaller {

  implicit def toEntityMarshallerCategories: ToEntityMarshaller[Categories]

  implicit def toEntityMarshallerInstitution: ToEntityMarshaller[Institution]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

  implicit def toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]

}

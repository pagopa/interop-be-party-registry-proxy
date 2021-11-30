package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Problem
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.projectName

class LoaderApi(
  loaderApiService: LoaderApiService,
  loaderMarshaller: LoaderApiMarshaller,
  wrappingDirective: Directive1[Unit]
) {

  import loaderMarshaller._

  lazy val route: Route =
    pathPrefix(projectName / buildinfo.BuildInfo.interfaceVersion) {
      path("reload") {
        get {
          wrappingDirective { _ =>
            parameters("cronExpression".as[String].?) { (cronExpression) =>
              loaderApiService.reloadData(cronExpression = cronExpression)
            }
          }
        }
      }
    }

}

trait LoaderApiService {
  def reloadData204: Route = complete((204, "Open data reloaded"))
  def reloadData400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: successful operation, DataType: Institution
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  def reloadData(cronExpression: Option[String])(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

}

trait LoaderApiMarshaller {
  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]
}

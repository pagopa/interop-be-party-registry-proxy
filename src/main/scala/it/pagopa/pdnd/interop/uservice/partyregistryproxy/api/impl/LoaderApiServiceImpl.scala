package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.LoaderApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution, Problem}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.server.impl.OpenDataLoader
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.{OpenDataService, SearchService}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class LoaderApiServiceImpl(
  openDataService: OpenDataService,
  institutionsSearchService: SearchService[Institution],
  categoriesSearchService: SearchService[Category]
)(implicit ec: ExecutionContext)
    extends LoaderApiService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: successful operation, DataType: Institution
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  override def reloadData()(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val result = for {
      _ <- OpenDataLoader.futureLoadOpenData(openDataService, institutionsSearchService, categoriesSearchService)
    } yield ()

    onComplete(result) {
      case Success(_) =>
        logger.info(s"Open data reloaded")
        reloadData204
      case Failure(ex) =>
        logger.error(s"Error trying to reload index, due: ${ex.getMessage}")
        val errorResponse: Problem = Problem(Option(ex.getMessage), 400, "some error")
        reloadData400(errorResponse)
    }
  }
}

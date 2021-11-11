package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.OpenDataService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util.{
  OpenDataResponse,
  OpenDataResponseMarshaller
}

import scala.concurrent.{ExecutionContext, Future}

final case class OpenDataServiceImpl(http: HttpExt)(implicit system: ActorSystem[Nothing], ec: ExecutionContext)
    extends OpenDataService
    with OpenDataResponseMarshaller {

  def getAllInstitutions: Future[List[Institution]] = {
    retrieveOpenData(ApplicationConfiguration.institutionsOpenDataUrl).map(OpenDataService.extractInstitutions)
  }

  override def getAllCategories: Future[List[Category]] = {
    retrieveOpenData(ApplicationConfiguration.categoriesOpenDataUrl).map(OpenDataService.extractCategories)
  }

  private def retrieveOpenData(uri: String): Future[OpenDataResponse] = {
    for {
      response         <- http.singleRequest(HttpRequest(uri = uri))
      openDataResponse <- Unmarshal(response).to[OpenDataResponse]
    } yield openDataResponse
  }
}

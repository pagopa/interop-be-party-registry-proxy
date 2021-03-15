package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onSuccess
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.persistence.InstitutionsPersistentBehavior.{
  Command,
  GetInstitution,
  Search
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{ErrorResponse, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system._

import scala.concurrent.Future

class InstitutionApiServiceImpl(commander: ActorSystem[Command]) extends InstitutionApiService {

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result: Future[StatusReply[Option[Institution]]] = commander.ask(ref => GetInstitution(institutionId, ref))

    val errorResponse: ErrorResponse = ErrorResponse(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getInstitutionById404(errorResponse))(institution => getInstitutionById200(institution))
    }
  }

  /** Code: 200, Message: successful operation, DataType: Seq[Institution]
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def searchInstitution(search: String)(implicit
    toEntityMarshallerInstitutionarray: ToEntityMarshaller[Seq[Institution]],
    toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result: Future[StatusReply[List[Institution]]] = commander.ask(ref => Search(search, ref))

    onSuccess(result) { statusReply =>
      searchInstitution200(statusReply.getValue)
    }
  }
}

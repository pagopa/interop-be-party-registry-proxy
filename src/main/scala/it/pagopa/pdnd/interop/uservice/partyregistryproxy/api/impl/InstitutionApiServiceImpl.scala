package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onSuccess
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.InstitutionApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system._
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.persistence.InstitutionsPersistentBehavior.{
  Command,
  GetInstitution,
  Search
}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Institution, Institutions, Problem}

import scala.concurrent.Future

class InstitutionApiServiceImpl(commander: ActorSystem[Command]) extends InstitutionApiService {

  /** Code: 200, Message: successful operation, DataType: InstitutionIPA
    * Code: 400, Message: Invalid ID supplied, DataType: ErrorResponse
    * Code: 404, Message: Institution not found, DataType: ErrorResponse
    */
  override def getInstitutionById(institutionId: String)(implicit
    toEntityMarshallerInstitutionIPA: ToEntityMarshaller[Institution],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val result: Future[StatusReply[Option[Institution]]] = commander.ask(ref => GetInstitution(institutionId, ref))

    val errorResponse: Problem = Problem(detail = None, status = 404, title = "some error")
    onSuccess(result) { statusReply =>
      statusReply.getValue.fold(getInstitutionById404(errorResponse))(institution => getInstitutionById200(institution))
    }
  }

  /** Code: 200, Message: successful operation, DataType: Institutions
    * Code: 400, Message: Invalid ID supplied, DataType: Problem
    * Code: 404, Message: Institution not found, DataType: Problem
    */
  override def searchInstitution(search: String, offset: Int, limit: Int)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerInstitutions: ToEntityMarshaller[Institutions]
  ): Route = {

    val result: Future[StatusReply[List[Institution]]] = commander.ask(ref => Search(search, offset, limit, ref))

    onSuccess(result) { statusReply =>
      searchInstitution200(Institutions(statusReply.getValue))
    }
  }

}

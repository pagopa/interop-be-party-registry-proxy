package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Problem

class HealthApiServiceImpl extends HealthApiService {

  override def getStatus()(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val response: Problem = Problem(
      `type` = "about:blank",
      status = StatusCodes.OK.intValue,
      title = StatusCodes.OK.defaultMessage,
      detail = None,
      errors = Seq.empty
    )
    getStatus200(response)
  }
}

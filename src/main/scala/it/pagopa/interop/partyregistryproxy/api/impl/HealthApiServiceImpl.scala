package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.pagopa.interop.partyregistryproxy.api.HealthApiService
import it.pagopa.interop.partyregistryproxy.model.Problem

class HealthApiServiceImpl extends HealthApiService {

  override def getStatus()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = getStatus200(
    Problem(
      `type` = "about:blank",
      status = StatusCodes.OK.intValue,
      title = StatusCodes.OK.defaultMessage,
      detail = None,
      errors = Seq.empty
    )
  )

}

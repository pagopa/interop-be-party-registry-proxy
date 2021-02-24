package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.HealthApiService
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Status

class HealthApiServiceImpl extends HealthApiService {

  override def getStatus200(responseStatus: Status)(implicit
    toEntityMarshallerStatus: ToEntityMarshaller[Status]
  ): Route = ???

  override def getStatus()(implicit toEntityMarshallerStatus: ToEntityMarshaller[Status]): Route = ???

}

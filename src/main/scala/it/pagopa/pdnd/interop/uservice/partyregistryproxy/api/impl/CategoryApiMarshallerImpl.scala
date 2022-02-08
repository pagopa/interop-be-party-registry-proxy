package it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.api.CategoryApiMarshaller
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Categories, Category, Problem}

object CategoryApiMarshallerImpl extends CategoryApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerCategory: ToEntityMarshaller[Category] = sprayJsonMarshaller

  override implicit def toEntityMarshallerCategories: ToEntityMarshaller[Categories] = sprayJsonMarshaller

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller
}

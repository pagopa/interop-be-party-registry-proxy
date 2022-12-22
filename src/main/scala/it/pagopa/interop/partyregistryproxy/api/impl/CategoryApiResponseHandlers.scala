package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._

import scala.util.{Failure, Success, Try}

object CategoryApiResponseHandlers extends AkkaResponses {

  def getCategoriesResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def getCategoryResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                    => success(s)
      case Failure(ex: CategoryNotFound) => notFound(ex, logMessage)
      case Failure(ex)                   => internalServerError(ex, logMessage)
    }

}

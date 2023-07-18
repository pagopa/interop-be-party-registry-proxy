package it.pagopa.interop.partyregistryproxy.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses
import it.pagopa.interop.partyregistryproxy.errors.PartyRegistryProxyErrors._

object AooApiResponseHandlers extends AkkaResponses {

  def getAOOByIdResponse[T](logMessage: String)(success: T => Route)(
    result: Either[Throwable, T]
  )(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Right(s)              => success(s)
      case Left(ex: AOONotFound) => notFound(ex, logMessage)
      case Left(ex)              => internalServerError(ex, logMessage)
    }

  def getAOOByExternalIdResponse[T](logMessage: String)(success: T => Route)(
    result: Either[Throwable, T]
  )(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Right(s)                        => success(s)
      case Left(ex: AOOByExternalNotFound) => notFound(ex, logMessage)
      case Left(ex)                        => internalServerError(ex, logMessage)
    }

  def searchAOOResponse[T](logMessage: String)(success: T => Route)(
    result: Either[Throwable, T]
  )(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Right(s) => success(s)
      case Left(ex) => internalServerError(ex, logMessage)
    }

}

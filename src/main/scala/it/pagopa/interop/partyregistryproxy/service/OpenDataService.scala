package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.common.util.InstitutionDetails
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}

import scala.concurrent.Future

trait OpenDataService {
  def getAllInstitutions(
    institutionsDetails: Map[String, InstitutionDetails],
    institutionKind: InstitutionKind
  ): Future[List[Institution]]
  def getAllCategories: Future[List[Category]]
}

sealed trait InstitutionKind

object InstitutionKind {
  case object Agency extends InstitutionKind
  case object AOO    extends InstitutionKind
  case object UO     extends InstitutionKind
}

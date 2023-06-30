package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}

import scala.concurrent.Future

trait OpenDataService {
  def getAllInstitutions(
    categorySource: Map[String, String],
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

package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}

import scala.concurrent.Future

trait OpenDataService {
  def getAllInstitutions: Future[List[Institution]]
  def getAllCategories: Future[List[Category]]
}

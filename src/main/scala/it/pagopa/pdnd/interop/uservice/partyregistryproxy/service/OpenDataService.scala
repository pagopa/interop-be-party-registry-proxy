package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}

import scala.concurrent.Future

trait OpenDataService {
  def getAllInstitutions: Future[List[Institution]]
  def getAllCategories: Future[List[Category]]
}

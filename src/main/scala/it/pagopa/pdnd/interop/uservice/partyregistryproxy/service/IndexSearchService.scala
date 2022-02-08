package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.SearchField

import scala.util.Try

trait IndexSearchService[A] {
  def searchById(id: String): Try[Option[A]]
  def searchByText(searchingField: String, searchText: String, page: Int, limit: Int): Try[(List[A], Long)]
  //TODO add pagination, low priority
  def getAllItems(filters: Map[SearchField, String]): Try[List[A]]

}

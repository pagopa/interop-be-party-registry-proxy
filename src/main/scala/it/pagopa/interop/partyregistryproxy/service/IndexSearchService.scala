package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.common.util.SearchField

import scala.util.Try

trait IndexSearchService[A] {
  def searchById(id: String): Try[Option[A]]
  def searchByExternalId(origin: String, originId: String): Try[Option[A]]
  def searchByText(searchingField: String, searchText: String, page: Int, limit: Int): Try[(List[A], Long)]
  def getAllItems(filters: Map[SearchField, String], page: Int, limit: Int): Try[(List[A], Long)]

}

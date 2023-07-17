package it.pagopa.interop.partyregistryproxy.service

import it.pagopa.interop.partyregistryproxy.common.util.SearchField

trait IndexSearchService[A] {
  def searchById(id: String): Either[Throwable, Option[A]]
  def searchByExternalId(origin: String, originId: String): Either[Throwable, Option[A]]
  def searchByText(
    searchingField: String,
    searchText: String,
    page: Int,
    limit: Int
  ): Either[Throwable, (List[A], Long)]
  def getAllItems(filters: Map[SearchField, String], page: Int, limit: Int): Either[Throwable, (List[A], Long)]

}

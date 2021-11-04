package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import scala.util.Try

trait SearchService[A] {
  def adds(items: List[A]): Try[Unit]
  def searchById(id: String): Try[Option[A]]
  def searchByText(searchText: String, searchingField: String, page: Int, limit: Int): Try[(List[A], Long)]
  def getAllItems: Try[List[A]]
  def deleteAll(): Try[Long]
  def commit(): Long

}

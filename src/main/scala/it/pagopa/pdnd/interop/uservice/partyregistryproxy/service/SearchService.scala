package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution

import scala.util.Try

trait SearchService {
  def add(institution: Institution): Try[Long]
  def delete(institutionId: String): Try[Long]
  def adds(institutions: Iterator[Institution]): Try[Long]
  def deleteAll(): Try[Long]
  def searchByDescription(keyword: String, page: Int, limit: Int): Try[(List[Institution], Long)]
  def count(keyword: String): Try[Int]
  def searchById(id: String): Try[Option[Institution]]
  def commit(): Long
  def close(): Unit

}

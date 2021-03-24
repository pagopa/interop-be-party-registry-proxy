package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution

import scala.util.Try

trait LuceneService {
  def add(institution: Institution): Try[Long]
  def delete(institutionId: String): Try[Long]
  def adds(institutions: Iterator[Institution]): Try[Long]
  def deleteAll(): Try[Long]
  def searchByDescription(keyword: String): List[Institution]
  def count(keyword: String): Int
  def searchById(id: String): Option[Institution]
  def commit(): Long
  def close(): Unit

}

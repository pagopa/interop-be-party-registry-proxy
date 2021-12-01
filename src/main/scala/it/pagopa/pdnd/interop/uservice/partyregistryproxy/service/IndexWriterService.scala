package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import scala.util.Try

trait IndexWriterService[A] {
  def adds(items: List[A]): Try[Unit]
  def deleteAll(): Try[Long]
  def commit(): Try[Unit]
}

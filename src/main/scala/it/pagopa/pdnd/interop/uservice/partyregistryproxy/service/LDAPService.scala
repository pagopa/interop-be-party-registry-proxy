package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution

trait LDAPService {
  def getAllInstitutions: Iterator[Institution]
}

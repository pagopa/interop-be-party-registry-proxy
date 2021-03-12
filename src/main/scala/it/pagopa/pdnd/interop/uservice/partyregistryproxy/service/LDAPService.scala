package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.InstitutionIPA

trait LDAPService {
  def getAllInstitutions: Iterator[InstitutionIPA]
}

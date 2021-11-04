package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.{CategoryFields, InstitutionFields}
import org.apache.lucene.document.Document

trait DocumentConverter[A] {
  def to(document: Document): A
}

object DocumentConverter {
  def to[A: DocumentConverter](document: Document): A = implicitly[DocumentConverter[A]].to(document)

  implicit def institutionConverter: DocumentConverter[Institution] = (document: Document) => {
    Institution(
      id = document.get(InstitutionFields.ID),
      o = Option(document.get(InstitutionFields.O)),
      ou = Option(document.get(InstitutionFields.OU)),
      aoo = Option(document.get(InstitutionFields.AOO)),
      fiscalCode = document.get(InstitutionFields.FISCAL_CODE),
      category = document.get(InstitutionFields.CATEGORY),
      managerName = Option(document.get(InstitutionFields.MANAGER_NAME)),
      managerSurname = Option(document.get(InstitutionFields.MANAGER_SURNAME)),
      description = document.get(InstitutionFields.DESCRIPTION),
      digitalAddress = document.get(InstitutionFields.DIGITAL_ADDRESS)
    )
  }

  implicit def categoryConverter: DocumentConverter[Category] = (document: Document) => {
    Category(
      code = document.get(CategoryFields.CODE),
      name = document.get(CategoryFields.NAME),
      kind = document.get(CategoryFields.KIND)
    )
  }
}

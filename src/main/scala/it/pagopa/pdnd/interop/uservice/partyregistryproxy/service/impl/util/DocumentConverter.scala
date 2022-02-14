package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl.util

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util.{CategoryField, InstitutionField}
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.document.Document

trait DocumentConverter[A] {
  def to(document: Document): A
}

object DocumentConverter {
  def to[A: DocumentConverter](document: Document): A = implicitly[DocumentConverter[A]].to(document)

  implicit def institutionConverter: DocumentConverter[Institution] = (document: Document) => {
    Institution(
      id = document.get(InstitutionField.ID.value),
      o = Option(document.get(InstitutionField.O.value)),
      ou = Option(document.get(InstitutionField.OU.value)),
      aoo = Option(document.get(InstitutionField.AOO.value)),
      taxCode = document.get(InstitutionField.TAX_CODE.value),
      category = document.get(InstitutionField.CATEGORY.value),
      description = document.get(InstitutionField.DESCRIPTION.value),
      digitalAddress = document.get(InstitutionField.DIGITAL_ADDRESS.value),
      address = document.get(InstitutionField.ADDRESS.value),
      zipCode = document.get(InstitutionField.ZIP_CODE.value),
      origin = document.get(InstitutionField.ORIGIN.value)
    )
  }

  implicit def categoryConverter: DocumentConverter[Category] = (document: Document) => {
    Category(
      code = document.get(CategoryField.CODE.value),
      name = document.get(CategoryField.NAME.value),
      kind = document.get(CategoryField.KIND.value),
      origin = document.get(CategoryField.ORIGIN.value)
    )
  }
}

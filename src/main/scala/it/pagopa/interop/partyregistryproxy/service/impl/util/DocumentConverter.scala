package it.pagopa.interop.partyregistryproxy.service.impl.util

import it.pagopa.interop.partyregistryproxy.common.util.CategoryField
import it.pagopa.interop.partyregistryproxy.common.util.CategoryField.{CODE, KIND, NAME}
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField._
import it.pagopa.interop.partyregistryproxy.model.{Category, Institution}
import org.apache.lucene.document.Document

trait DocumentConverter[A] {
  def to(document: Document): A
}

object DocumentConverter {
  def to[A](document: Document)(implicit dc: DocumentConverter[A]): A = dc.to(document)

  implicit def institutionConverter: DocumentConverter[Institution] = (document: Document) => {
    Institution(
      id = document.get(ID.value),
      originId = document.get(ORIGIN_ID.value),
      taxCode = document.get(TAX_CODE.value),
      category = document.get(CATEGORY.value),
      description = document.get(DESCRIPTION.value),
      digitalAddress = document.get(DIGITAL_ADDRESS.value),
      address = document.get(ADDRESS.value),
      zipCode = document.get(ZIP_CODE.value),
      origin = document.get(ORIGIN.value),
      kind = document.get(KIND.value)
    )
  }

  implicit def categoryConverter: DocumentConverter[Category] = (document: Document) =>
    Category(
      code = document.get(CODE.value),
      name = document.get(NAME.value),
      kind = document.get(KIND.value),
      origin = document.get(CategoryField.ORIGIN.value)
    )

}

package it.pagopa.interop.partyregistryproxy.service.impl.util

import it.pagopa.interop.partyregistryproxy.common.util.CategoryField
import it.pagopa.interop.partyregistryproxy.common.util.InstitutionField
import it.pagopa.interop.partyregistryproxy.model.{Category, Classification, Institution}
import org.apache.lucene.document.Document

trait DocumentConverter[A] {
  def to(document: Document): Either[Throwable, A]
}

object DocumentConverter {
  def to[A](document: Document)(implicit dc: DocumentConverter[A]): Either[Throwable, A] = dc.to(document)

  implicit def institutionConverter: DocumentConverter[Institution] = (document: Document) =>
    Classification.fromValue(document.get(InstitutionField.CLASSIFICATION.value)).map { classification =>
      Institution(
        id = document.get(InstitutionField.ID.value),
        originId = document.get(InstitutionField.ORIGIN_ID.value),
        taxCode = document.get(InstitutionField.TAX_CODE.value),
        category = document.get(InstitutionField.CATEGORY.value),
        description = document.get(InstitutionField.DESCRIPTION.value),
        digitalAddress = document.get(InstitutionField.DIGITAL_ADDRESS.value),
        address = document.get(InstitutionField.ADDRESS.value),
        zipCode = document.get(InstitutionField.ZIP_CODE.value),
        origin = document.get(InstitutionField.ORIGIN.value),
        kind = document.get(InstitutionField.KIND.value),
        classification = classification
      )
    }

  implicit def categoryConverter: DocumentConverter[Category] = (document: Document) =>
    Right(
      Category(
        code = document.get(CategoryField.CODE.value),
        name = document.get(CategoryField.NAME.value),
        kind = document.get(CategoryField.KIND.value),
        origin = document.get(CategoryField.ORIGIN.value)
      )
    )

}

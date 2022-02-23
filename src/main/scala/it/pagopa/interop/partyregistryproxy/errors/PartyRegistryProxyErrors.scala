package it.pagopa.interop.partyregistryproxy.errors

import it.pagopa.interop.commons.utils.errors.ComponentError

object PartyRegistryProxyErrors {

  final case object InvalidGetInstitutionRequest extends ComponentError("0001", "Invalid request")
  final case object InstitutionNotFound          extends ComponentError("0002", "Institution not found")

  final case object InvalidSearchInstitutionRequest extends ComponentError("0003", "Invalid request")
  final case object InstitutionsNotFound            extends ComponentError("0004", "Institutions not found")

  final case object CategoriesNotFound extends ComponentError("0005", "No categories found")
  final case object CategoriesError    extends ComponentError("0006", "Error while retrieving categories")
  final case class CategoryNotFound(categoryCode: String)
      extends ComponentError("0007", s"Category $categoryCode not found")
}

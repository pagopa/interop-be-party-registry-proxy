package it.pagopa.interop.partyregistryproxy.errors

import it.pagopa.interop.commons.utils.errors.ComponentError

object PartyRegistryProxyErrors {

  final case class InstitutionNotFound(institutionId: String)
      extends ComponentError("0001", s"Institution $institutionId not found")

  final case class CategoryNotFound(categoryCode: String)
      extends ComponentError("0002", s"Category $categoryCode not found")
  final case class CategoriesError(error: String)
      extends ComponentError("0003", s"Error while retrieving categories - $error")

  final case class InstitutionByExternalNotFound(origin: String, originId: String)
      extends ComponentError("0004", s"Institution with Origin $origin and OriginId$originId not found")

  final case class AOONotFound(aooId: String) extends ComponentError("0005", s"AOO $aooId not found")

  final case class AOOByExternalNotFound(origin: String, originId: String)
      extends ComponentError("0006", s"AOO with Origin $origin and OriginId$originId not found")

  final case class UONotFound(uoId: String) extends ComponentError("0007", s"UO $uoId not found")

  final case class UOByExternalNotFound(origin: String, originId: String)
      extends ComponentError("0008", s"UO with Origin $origin and OriginId$originId not found")

}

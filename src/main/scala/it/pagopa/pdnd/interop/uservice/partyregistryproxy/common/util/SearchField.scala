package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.util

sealed trait SearchField {
  def value: String
}

abstract class CategoryField(val value: String) extends SearchField

object CategoryField {
  case object ID     extends CategoryField("id")
  case object CODE   extends CategoryField("code")
  case object NAME   extends CategoryField("name")
  case object KIND   extends CategoryField("kind")
  case object ORIGIN extends CategoryField("origin")
}

abstract class InstitutionField(val value: String) extends SearchField

object InstitutionField {
  case object ID                  extends CategoryField("id")
  case object O                   extends CategoryField("o")
  case object OU                  extends CategoryField("ou")
  case object AOO                 extends CategoryField("aoo")
  case object TAX_CODE            extends CategoryField("taxCode")
  case object CATEGORY            extends CategoryField("category")
  case object MANAGER_GIVEN_NAME  extends CategoryField("managerGivenName")
  case object MANAGER_FAMILY_NAME extends CategoryField("managerFamilyName")
  case object DESCRIPTION         extends CategoryField("description")
  case object DIGITAL_ADDRESS     extends CategoryField("digitalAddress")
  case object ORIGIN              extends CategoryField("origin")

}

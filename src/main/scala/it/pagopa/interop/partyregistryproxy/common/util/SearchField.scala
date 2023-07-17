package it.pagopa.interop.partyregistryproxy.common.util

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
  case object ID              extends CategoryField("id")
  case object ORIGIN_ID       extends CategoryField("originId")
  case object O               extends CategoryField("o")
  case object OU              extends CategoryField("ou")
  case object AOO             extends CategoryField("aoo")
  case object TAX_CODE        extends CategoryField("taxCode")
  case object CATEGORY        extends CategoryField("category")
  case object DESCRIPTION     extends CategoryField("description")
  case object DIGITAL_ADDRESS extends CategoryField("digitalAddress")
  case object ADDRESS         extends CategoryField("address")
  case object ZIP_CODE        extends CategoryField("zipCode")
  case object ORIGIN          extends CategoryField("origin")
  case object KIND            extends CategoryField("kind")
  case object CLASSIFICATION  extends CategoryField("classification")

}

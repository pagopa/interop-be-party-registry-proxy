package it.pagopa.pdnd.interop.uservice.partyregistryproxy.model

/** @param id iPA code for example: ''age''
  * @param o o for example: ''age''
  * @param ou ou for example: ''age''
  * @param aoo aoo for example: ''age''
  * @param fiscalCode institution fiscal code for example: ''00000000000''
  * @param category institution category for example: ''c7''
  * @param managerName manager name for example: ''Mario''
  * @param managerSurname manager surname for example: ''Rossi''
  * @param description institution description for example: ''AGENCY X''
  * @param digitalAddress digital institution address for example: ''mail@pec.mail.org''
  */
final case class Institution(
  id: String,
  o: Option[String],
  ou: Option[String],
  aoo: Option[String],
  fiscalCode: String,
  category: String,
  managerName: Option[String],
  managerSurname: Option[String],
  description: String,
  digitalAddress: String
)

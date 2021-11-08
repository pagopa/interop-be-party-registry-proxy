package it.pagopa.pdnd.interop.uservice.partyregistryproxy.model


/**
 * @param items  for example: ''null''
 * @param count  for example: ''null''
*/
final case class Institutions (
  items: Seq[Institution],
  count: Long
)


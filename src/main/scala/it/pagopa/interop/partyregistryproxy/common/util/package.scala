package it.pagopa.interop.partyregistryproxy.common

package object util {
  def createCategoryId(origin: String, code: String): String = s"${origin}_${code}"
}

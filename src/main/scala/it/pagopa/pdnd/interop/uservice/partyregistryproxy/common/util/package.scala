package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common

package object util {
  def createCategoryId(code: String, origin: String): String = {
    s"${code}_$origin"
  }
}

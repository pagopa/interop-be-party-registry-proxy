package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common

package object util {
  def createCategoryId(origin: String, code: String): String = {
    s"${origin}_${code}"
  }
}

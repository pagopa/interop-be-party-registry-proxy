package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import javax.naming.directory.SearchResult

package object impl {
  implicit class SearchResultOps(val result: SearchResult) extends AnyVal {
    def extract(attributeName: String): Option[String] = {
      Option(result.getAttributes.get(attributeName)).map(_.get().asInstanceOf[String])
//        .orElse {
//        println(attributeName)
//        None
//      }
    }
  }

}

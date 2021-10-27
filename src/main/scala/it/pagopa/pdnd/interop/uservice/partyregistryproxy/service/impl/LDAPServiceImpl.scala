package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.LDAPService

import javax.naming.NamingEnumeration
import javax.naming.directory.{DirContext, SearchControls, SearchResult}
import scala.jdk.CollectionConverters._
import scala.util.Try

final case class LDAPServiceImpl(connection: DirContext) extends LDAPService {

  def getAllInstitutions: Iterator[Institution] = {
    val searchFilter: String = "(objectClass=*)"

    val searchControls: SearchControls = new SearchControls()
    val reqAtt: Array[String] =
      Array(
        "objectClass",
        "codiceFiscaleAmm",
        "mail",
        "description",
        "nomeResp",
        "cognomeResp",
        "tipoAmm",
        "o",
        "aoo",
        "ou"
      )

    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE)
    searchControls.setReturningAttributes(reqAtt)

    val results: NamingEnumeration[SearchResult] = connection.search("c=it", searchFilter, searchControls)

    results.asScala.flatMap { result =>
      fromResult(result).toOption
    }

  }

  def fromResult(result: SearchResult): Try[Institution] = Try {
    val id: String          = result.getNameInNamespace.replace("dn: ", "")
    val o: Option[String]   = result.extract("o").map(_.replace("o: ", ""))
    val aoo: Option[String] = result.extract("aoo").map(_.replace("aoo: ", ""))
    val ou: Option[String]  = result.extract("ou").map(_.replace("ou: ", ""))

    Institution(
      id = id,
      o = o,
      ou = ou,
      aoo = aoo,
      taxCode = result.extract("codiceFiscaleAmm"),
      administrationCode = result.extract("codiceAmm"),
      category = result.extract("tipoAmm"),
      managerName = result.extract("nomeResp"),
      managerSurname = result.extract("cognomeResp"),
      description = result.extract("description").get,
      digitalAddress = result.extract("mail")
    )
  }

}
object LDAPServiceImpl {
  def create(connection: DirContext): LDAPServiceImpl = new LDAPServiceImpl(connection)

}

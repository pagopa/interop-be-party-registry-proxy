package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.impl

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.InstitutionIPA
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.service.LDAPService

import java.util.{Properties, UUID}
import javax.naming.directory.{DirContext, InitialDirContext, SearchControls, SearchResult}
import javax.naming.{Context, NamingEnumeration}
import scala.jdk.CollectionConverters._
import scala.util.Try

final case class LDAPServiceImpl(connection: DirContext) extends LDAPService {

  def getAllInstitutions: Iterator[InstitutionIPA] = {

    val searchFilter: String = "(objectClass=*)"

    val searchControls: SearchControls = new SearchControls()
    val reqAtt: Array[String] =
      Array("objectClass", "codiceFiscaleAmm", "mail", "description", "nomeResp", "cognomeResp", "tipoAmm", "o")

    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE)
    searchControls.setReturningAttributes(reqAtt)

    val results: NamingEnumeration[SearchResult] = connection.search("c=it", searchFilter, searchControls)

    results.asScala.flatMap { result =>
      fromResult(result).toOption
    }

  }

  def fromResult(result: SearchResult): Try[InstitutionIPA] = Try {
    InstitutionIPA(
      id = UUID.randomUUID(),
      externalId = result.getAttributes.get("o").toString,
      taxCode = result.getAttributes.get("codiceFiscaleAmm").toString,
      managerTaxCode = "???", //result.getAttributes.get("o"),
      managerName = Option(result.getAttributes.get("nomeResp")) map (_.toString),
      managerSurname = Option(result.getAttributes.get("cognomeResp")) map (_.toString),
      description = result.getAttributes.get("description").toString,
      digitalAddress = result.getAttributes.get("mail").toString
    )
  }

}

object LDAPServiceImpl {
  def apply(connection: DirContext): LDAPServiceImpl = new LDAPServiceImpl(connection)

  def createConnection(url: String, userName: String, password: String): Option[DirContext] = {
    val properties: Properties = new Properties()

    properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
    properties.put(Context.PROVIDER_URL, url)
    properties.put(Context.SECURITY_PRINCIPAL, userName)
    properties.put(Context.SECURITY_CREDENTIALS, password)

    Try(new InitialDirContext(properties)).toOption

  }
}

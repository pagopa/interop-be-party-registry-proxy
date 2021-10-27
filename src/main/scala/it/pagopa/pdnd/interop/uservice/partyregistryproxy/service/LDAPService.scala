package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution

import java.util.Properties
import javax.naming.Context
import javax.naming.directory.{DirContext, InitialDirContext}
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

trait LDAPService {
  def getAllInstitutions: Iterator[Institution]
}

object LDAPService {
  def createConnection(vault: Vault): Try[DirContext] = {
    val data     = vault.logical().read("secret/data/pdnd-interop-dev/service-party-registry-proxy/ldpad")
    val url      = data.getData.asScala("url")
    val username = data.getData.asScala("username")
    val password = data.getData.asScala("password")

    val properties: Properties = new Properties()

    properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
    properties.put(Context.PROVIDER_URL, url)
    properties.put(Context.SECURITY_PRINCIPAL, username)
    properties.put(Context.SECURITY_CREDENTIALS, password)

    Try(new InitialDirContext(properties))

  }
}

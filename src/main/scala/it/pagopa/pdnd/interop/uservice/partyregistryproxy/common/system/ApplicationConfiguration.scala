package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-party-registry-proxy.port")

  def institutionsIndexDir: String = config.getString("uservice-party-registry-proxy.index.institutions.folder")
  def institutionsIpaOpenDataUrl: String =
    config.getString("uservice-party-registry-proxy.ipa.institutions.open-data-url")

  def categoriesIndexDir: String       = config.getString("uservice-party-registry-proxy.index.categories.folder")
  def categoriesIpaOpenDataUrl: String = config.getString("uservice-party-registry-proxy.ipa.categories.open-data-url")

  def ipaCronTime: String = config.getString("uservice-party-registry-proxy.ipa.ipa-update-time")
  def ipaOrigin: String   = config.getString("uservice-party-registry-proxy.ipa.origin")
}

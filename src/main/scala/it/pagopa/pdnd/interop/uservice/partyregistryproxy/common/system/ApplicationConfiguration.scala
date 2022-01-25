package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = config.getInt("uservice-party-registry-proxy.port")

  def institutionsIndexDir: String    = config.getString("uservice-party-registry-proxy.institutions.index-dir")
  def institutionsOpenDataUrl: String = config.getString("uservice-party-registry-proxy.institutions.open-data-url")

  def categoriesIndexDir: String    = config.getString("uservice-party-registry-proxy.categories.index-dir")
  def categoriesOpenDataUrl: String = config.getString("uservice-party-registry-proxy.categories.open-data-url")

  def cronTime: String = config.getString("uservice-party-registry-proxy.ipa-update-time")
}

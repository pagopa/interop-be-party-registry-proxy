package it.pagopa.interop.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("party-registry-proxy.port")

  lazy val jwtAudience: Set[String] = config.getStringList("party-registry-proxy.jwt.audience").asScala.toSet

  lazy val institutionsIndexDir: String       = config.getString("party-registry-proxy.index.institutions.folder")
  lazy val institutionsIpaOpenDataUrl: String = config.getString("party-registry-proxy.ipa.institutions.open-data-url")

  lazy val categoriesIndexDir: String       = config.getString("party-registry-proxy.index.categories.folder")
  lazy val categoriesIpaOpenDataUrl: String = config.getString("party-registry-proxy.ipa.categories.open-data-url")

  lazy val ipaOrigin: String = config.getString("party-registry-proxy.ipa.origin")
}

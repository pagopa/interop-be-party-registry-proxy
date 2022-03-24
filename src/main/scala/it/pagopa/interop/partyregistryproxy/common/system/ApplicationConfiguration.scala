package it.pagopa.interop.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("party-registry-proxy.port")

  lazy val institutionsIndexDir: String = config.getString("party-registry-proxy.index.institutions.folder")
  lazy val categoriesIndexDir: String   = config.getString("party-registry-proxy.index.categories.folder")

  lazy val institutionsIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.institutions.open-data-url")
  lazy val categoriesIpaOpenDataUrl: String   =
    config.getString("party-registry-proxy.sources.ipa.categories.open-data-url")
  lazy val ipaOrigin: String                  = config.getString("party-registry-proxy.sources.ipa.origin")

  lazy val institutionsMockOpenDataUrl: Option[String] = Try(
    config.getString("party-registry-proxy.sources.mock.institutions.open-data-url")
  ).toOption
  lazy val categoriesMockOpenDataUrl: Option[String]   = Try(
    config.getString("party-registry-proxy.sources.mock.categories.open-data-url")
  ).toOption
  lazy val mockOrigin: Option[String] = Try(config.getString("party-registry-proxy.mock.origin")).toOption

}

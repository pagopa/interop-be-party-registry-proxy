package it.pagopa.interop.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

object ApplicationConfiguration {
  System.setProperty("kanela.show-banner", "false")

  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("party-registry-proxy.port")

  val jwtAudience: Set[String] =
    config.getString("party-registry-proxy.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

  val institutionsIndexDir: String = config.getString("party-registry-proxy.index.institutions.folder")

  val categoriesIndexDir: String = config.getString("party-registry-proxy.index.categories.folder")

  val institutionsIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.institutions.open-data-url")

  val categoriesIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.categories.open-data-url")

  val ipaOrigin: String = config.getString("party-registry-proxy.sources.ipa.origin")

  val institutionsMockOpenDataUrl: Option[String] = Try(
    config.getString("party-registry-proxy.sources.mock.institutions.open-data-url")
  ).toOption

  val categoriesMockOpenDataUrl: Option[String] = Try(
    config.getString("party-registry-proxy.sources.mock.categories.open-data-url")
  ).toOption

  val mockOrigin: Option[String] = Try(config.getString("party-registry-proxy.sources.mock.origin")).toOption

}

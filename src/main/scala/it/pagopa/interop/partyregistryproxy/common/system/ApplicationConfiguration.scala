package it.pagopa.interop.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("party-registry-proxy.port")

  val jwtAudience: Set[String] =
    config.getString("party-registry-proxy.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

  val institutionsIndexDir: String = config.getString("party-registry-proxy.index.institutions.folder")

  val aooIndexDir: String = config.getString("party-registry-proxy.index.aoo.folder")

  val uooIndexDir: String = config.getString("party-registry-proxy.index.uo.folder")

  val categoriesIndexDir: String = config.getString("party-registry-proxy.index.categories.folder")

  val institutionsIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.institutions.open-data-url")

  val aooIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.aoo.open-data-url")

  val uoIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.uo.open-data-url")

  val categoriesIpaOpenDataUrl: String =
    config.getString("party-registry-proxy.sources.ipa.categories.open-data-url")

  val ipaOrigin: String = config.getString("party-registry-proxy.sources.ipa.origin")

}

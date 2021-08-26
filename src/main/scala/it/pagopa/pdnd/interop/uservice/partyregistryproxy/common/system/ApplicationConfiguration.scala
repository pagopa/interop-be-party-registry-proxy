package it.pagopa.pdnd.interop.uservice.partyregistryproxy.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int  = config.getInt("uservice-party-registry-proxy.port")
  def indexDir: String = config.getString("uservice-party-registry-proxy.index-dir")
  def cronTime: String = config.getString("uservice-party-registry-proxy.ipa-update-time")

}

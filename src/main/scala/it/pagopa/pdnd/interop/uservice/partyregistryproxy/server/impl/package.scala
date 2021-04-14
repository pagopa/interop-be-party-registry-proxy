package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}
import com.typesafe.config.{Config, ConfigFactory}

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.temporal.ChronoUnit

package object impl {

  def getInitialDelay(cronTime: String): Long = {
    val startTime: LocalTime = LocalTime.parse(cronTime)
    val today: LocalDate     = LocalDate.now()
    val next: LocalDateTime  = LocalDateTime.of(today, startTime)
    val now: LocalDateTime   = LocalDateTime.now()
    val diff: Long           = now.until(next, ChronoUnit.MILLIS)
    if (diff < 0) Math.abs(diff) else diff
  }

  lazy val vault: Vault = getVaultClient

  private def getVaultClient: Vault = {
    val vaultAddress = System.getenv("VAULT_ADDR")
    val vaultToken   = System.getenv("VAULT_TOKEN")
    val config = new VaultConfig()
      .address(vaultAddress)
      .token(vaultToken)
      .sslConfig(new SslConfig().verify(false).build())
      .build()
    new Vault(config)
  }

  lazy val config: Config = ConfigFactory.load()

}

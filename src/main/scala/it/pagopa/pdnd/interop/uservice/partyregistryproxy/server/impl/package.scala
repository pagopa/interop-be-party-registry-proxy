package it.pagopa.pdnd.interop.uservice.partyregistryproxy.server

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}

import java.time.LocalTime
import java.time.temporal.ChronoUnit

package object impl {

  def getInitialDelay(cronTime: String): Long = {
    val startTime: LocalTime = LocalTime.parse(cronTime)
    val now: LocalTime       = LocalTime.now()
    val diff: Long           = now.until(startTime, ChronoUnit.MILLIS)
    if (diff < 0) now.until(startTime.plusHours(24), ChronoUnit.MILLIS) else diff
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

}

package com.roughlyunderscore.underscorekillstreaks.hook

import com.roughlyunderscore.underscorekillstreaks.streakdata.Killstreak
import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class PAPIUKSMax : PlaceholderExpansion() {
  override fun getIdentifier(): String = "uksmax"
  override fun getAuthor(): String = "RoughlyUnderscore"
  override fun getVersion(): String = "1.1"
  override fun persist(): Boolean = true

  override fun onRequest(player: OfflinePlayer?, params: String): String {
    if (params.isEmpty()) return (UnderscoreKillstreaks.killstreaks[player?.uniqueId] ?: Killstreak(0, 0)).maxKills.toString()
    val detectedPlayer: OfflinePlayer = Bukkit.getOfflinePlayer(params)
    return (UnderscoreKillstreaks.killstreaks[detectedPlayer.uniqueId] ?: Killstreak(0, 0)).maxKills.toString()
  }
}
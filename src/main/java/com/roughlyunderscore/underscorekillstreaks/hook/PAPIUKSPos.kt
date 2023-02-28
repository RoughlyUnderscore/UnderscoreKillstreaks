package com.roughlyunderscore.underscorekillstreaks.hook

import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class PAPIUKSPos : PlaceholderExpansion() {
  override fun getIdentifier(): String = "ukspos"
  override fun getAuthor(): String = "RoughlyUnderscore"
  override fun getVersion(): String = "1.1"
  override fun persist(): Boolean = true

  override fun onRequest(player: OfflinePlayer?, params: String): String {
    val killstreaks = UnderscoreKillstreaks.killstreaks.toList().sortedByDescending { (_, value) -> value.maxKills }.toMap()

    if (params.isEmpty()) {
      val pos = killstreaks.keys.indexOf(player!!.uniqueId) + 1
      if (pos == 0) return "N/A"
      return pos.toString()
    }

    val detectedPlayer: OfflinePlayer = Bukkit.getOfflinePlayer(params)
    val pos = killstreaks.keys.indexOf(detectedPlayer.uniqueId) + 1
    if (pos == 0) return "N/A"
    return pos.toString()
  }
}
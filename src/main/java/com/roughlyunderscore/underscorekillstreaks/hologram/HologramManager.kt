package com.roughlyunderscore.underscorekillstreaks.hologram

import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import com.roughlyunderscore.underscorekillstreaks.streakdata.Killstreak
import com.roughlyunderscore.underscorekillstreaks.utils.Utils
import eu.decentsoftware.holograms.api.DHAPI
import eu.decentsoftware.holograms.api.DecentHologramsAPI
import eu.decentsoftware.holograms.api.holograms.Hologram
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class HologramManager(uks: JavaPlugin) {
  private val header: List<String> = uks.config.getStringList("leaderboard.header")
  private val footer: List<String> = uks.config.getStringList("leaderboard.footer")
  private val syntax: String = uks.config.getString("leaderboard.syntax") ?: "&6&l<position>. &d<player> &6&l- &d<streak>"
  private val size: Int = Utils.clamp(1, uks.config.getInt("leaderboard.size"), 15)
  private val displaySelf: Boolean = uks.config.getBoolean("leaderboard.displaySelf")
  private val prefixBeforeSelf: List<String> = uks.config.getStringList("leaderboard.prefixBeforeSelf")
  private val selfStreak: String = uks.config.getString("leaderboard.selfStreak") ?: "&6&l%ukspos%. &d%player_name% &6&l- &d%uksmax%"

  private val plugin = findHologramProvider()

  fun allHolograms() : Collection<Hologram> {
    return DecentHologramsAPI.get().hologramManager.holograms
  }

  private fun createHologram(location: Location, lines: List<String>, name: String) : Hologram {
    return when (plugin) {
      HologramPluginType.DECENT_HOLOGRAMS -> {
        val hologram = DHAPI.createHologram(name, location, true, lines)
        DecentHologramsAPI.get().hologramManager.registerHologram(hologram)
        hologram
      }
      HologramPluginType.NONE -> throw UnsupportedOperationException("No hologram plugin found")
    }
  }

  private fun deleteHologram(name: String) {
    when (plugin) {
      HologramPluginType.DECENT_HOLOGRAMS -> {
        DHAPI.getHologram(name)?.destroy()
      }
      HologramPluginType.NONE -> throw UnsupportedOperationException("No hologram plugin found")
    }
  }

  private fun deleteHologram(hologram: Hologram) {
    deleteHologram(hologram.name)
  }

  fun createLeaderboard(location: Location, name: String) : Hologram {
    return createHologram(location, fetchPreparedLines(), name)
  }

  fun updateLeaderboard(hologram: Hologram) {
    val lines = fetchPreparedLines()

    for (index in lines.indices) {
      hologram.pages.first().lines[index].text = lines[index]
    }

    hologram.updateAll()
    hologram.updateAnimationsAll()
  }

  fun getHologram(name: String) : Hologram? {
    return when (plugin) {
      HologramPluginType.DECENT_HOLOGRAMS -> {
        DHAPI.getHologram(name)
      }
      HologramPluginType.NONE -> throw UnsupportedOperationException("No hologram plugin found")
    }
  }

  private fun findHologramProvider() : HologramPluginType =
    if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null)
      HologramPluginType.DECENT_HOLOGRAMS
    else
      HologramPluginType.NONE


  private fun fetchSortedKillstreaks() : Map<UUID, Killstreak> =
    UnderscoreKillstreaks.killstreaks.toList().sortedByDescending { (_, value) -> value.maxKills }.toMap()

  private fun fetchMaxStreak() : Int = UnderscoreKillstreaks.killstreaks.values.maxByOrNull { it.maxKills }?.maxKills ?: 0

  private fun fetchPreparedLines() : List<String> {
    val lines = mutableListOf<String>()

    // Add all header lines
    lines.addAll(header)

    // Go through all players who have got killstreaks:
    // for each of them, find their position in the leaderboard,
    // their current killstreak and their max killstreak,
    // then add the syntax line with the replaced placeholders
    val killstreaks = fetchSortedKillstreaks()
    for (index in 1..size) {
      if (killstreaks.size < index) break
      val player = killstreaks.keys.elementAt(index - 1)
      val streak = killstreaks.values.elementAt(index - 1).kills
      val maxStreak = killstreaks.values.elementAt(index - 1).maxKills
      lines.add(syntax
        .replace("<position>", index.toString())
        .replace("<player>", Bukkit.getOfflinePlayer(player).name ?: "Unknown")
        .replace("<streak>", streak.toString())
        .replace("<max_streak>", maxStreak.toString()))
    }

    // If displaySelf is true, add the player's prefix & syntax line
    // to the leaderboard
    if (displaySelf) {
      lines.addAll(prefixBeforeSelf)
      lines.add(selfStreak) // This line only allows for PAPI, which is handled by DH
    }

    // Add the footer to the leaderboard
    lines.addAll(footer)

    // <max_streak_ever> is accessible everywhere,
    // so replace it throughout the entire list
    lines.replaceAll { it.replace("<max_streak_ever>", fetchMaxStreak().toString()) }

    return lines
  }
}
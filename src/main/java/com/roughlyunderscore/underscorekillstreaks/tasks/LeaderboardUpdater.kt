package com.roughlyunderscore.underscorekillstreaks.tasks

import com.roughlyunderscore.underscorekillstreaks.hologram.HologramManager
import eu.decentsoftware.holograms.api.DecentHologramsAPI
import eu.decentsoftware.holograms.api.holograms.Hologram
import org.bukkit.scheduler.BukkitRunnable

class LeaderboardUpdater(private val hologram: Hologram, private val hologramManager: HologramManager) : BukkitRunnable() {
  override fun run() {
    if (DecentHologramsAPI.get().hologramManager.getHologram(hologram.name) == null) {
      cancel()
      return
    }
    hologramManager.updateLeaderboard(hologram)
  }
}
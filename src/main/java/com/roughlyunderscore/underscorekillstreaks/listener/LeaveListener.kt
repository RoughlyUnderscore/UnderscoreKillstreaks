package com.roughlyunderscore.underscorekillstreaks.listener

import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class LeaveListener(plugin: UnderscoreKillstreaks) : Listener {
  private val reset: Boolean = plugin.config.getBoolean("resetOnLeave")

  @EventHandler
  fun onLeave(ev: PlayerQuitEvent) {
    if (!reset) return
    // Remove the player from the killstreaks map
    if (UnderscoreKillstreaks.killstreaks.containsKey(ev.player.uniqueId))
      UnderscoreKillstreaks.killstreaks[ev.player.uniqueId]?.kills = 0
  }
}
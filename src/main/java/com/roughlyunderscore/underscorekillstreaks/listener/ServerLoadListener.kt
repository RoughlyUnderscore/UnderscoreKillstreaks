package com.roughlyunderscore.underscorekillstreaks.listener

import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent

class ServerLoadListener(private val plugin: UnderscoreKillstreaks) : Listener {
  @EventHandler
  fun onLoad(event: ServerLoadEvent) {
    plugin.server.scheduler.runTaskLater(plugin, Runnable {
      plugin.initTasks()
    }, 20L)
  }
}
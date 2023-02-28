package com.roughlyunderscore.underscorekillstreaks.listener

import com.roughlyunderscore.underscorekillstreaks.Constants
import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import com.roughlyunderscore.underscorekillstreaks.streakdata.Killstreak
import com.roughlyunderscore.underscorekillstreaks.streakdata.Milestone
import com.roughlyunderscore.underscorekillstreaks.utils.Message
import com.roughlyunderscore.underscorekillstreaks.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

class ManagementListener() : Listener {
  private lateinit var conf: FileConfiguration
  private lateinit var plugin: UnderscoreKillstreaks
  constructor(plugin: UnderscoreKillstreaks) : this() {
    this.plugin = plugin
    conf = plugin.config
  }

  @EventHandler
  fun onDeath(ev: PlayerDeathEvent) {
    // If the killer is null, return
    if (ev.entity.killer == null) return

    val victim = ev.entity
    val player = victim.killer!!
    val uuid = player.uniqueId
    
    // If the world/player is blacklisted, return
    if (plugin.worldBlacklist.contains(victim.world)) return
    if (plugin.playerBlacklist.contains(victim.name) || plugin.playerBlacklist.contains(victim.displayName)) return
    if (plugin.playerBlacklist.contains(player.name) || plugin.playerBlacklist.contains(player.displayName)) return
    
    // Permission check
    if (!victim.hasPermission(Constants.STREAKABLE_PERM) || !player.hasPermission(Constants.STREAKABLE_PERM)) return
    
    // Remove the victim from the killstreaks map, as the streak is reset on death
    val victimKs = UnderscoreKillstreaks.killstreaks[victim.uniqueId]
    if (victimKs != null) UnderscoreKillstreaks.killstreaks[victim.uniqueId] = Killstreak(0, victimKs.maxKills)

    

    // Create/change the killstreak & max killstreak
    if (UnderscoreKillstreaks.killstreaks.containsKey(uuid)) {
      val ks = UnderscoreKillstreaks.killstreaks.remove(uuid)!!
      // If a player's increased kill count is higher than their current all-time high, update it
      if (ks.kills + 1 > ks.maxKills) UnderscoreKillstreaks.killstreaks[uuid] = Killstreak(ks.kills + 1, ks.kills + 1)
      // Otherwise, just update the kill count
      else UnderscoreKillstreaks.killstreaks[uuid] = Killstreak(ks.kills + 1, ks.maxKills)
    } else {
      UnderscoreKillstreaks.killstreaks[uuid] = Killstreak(1, 1)
    }

    val kills = UnderscoreKillstreaks.killstreaks[uuid]!!.kills
    val killsString = kills.toString()
    val maxKills = UnderscoreKillstreaks.killstreaks[uuid]!!.maxKills
    val maxKillsString = maxKills.toString()
    val weaponName = fetchLocalizedName(player.inventory.itemInMainHand, conf.getString("hand")!!)
    val decoloredWeaponName = ChatColor.stripColor(weaponName)!!
    val playerName = player.displayName
    val entityName = victim.displayName



    // Hacky simplification
    fun defaultPlaceholders(message: String) : String {
      return Message.Builder(message)
        .placeholder("<player>", playerName)
        .placeholder("<weapon>", weaponName)
        .placeholder("<nc_weapon>", decoloredWeaponName)
        .placeholder("<streak>", killsString)
        .placeholder("<max_streak>", maxKillsString)
        .papi(player)
        .format()
        .build()
        .message
    }

    fun breakerPlaceholders(message: String) : String {
      return Message.Builder(message)
        .placeholder("<breaker_player>", playerName)
        .placeholder("<broken_player>", entityName)
        .placeholder("<weapon>", weaponName)
        .placeholder("<nc_weapon>", decoloredWeaponName)
        .placeholder("<breaker_player_streak>", killsString)
        .placeholder("<breaker_player_max_streak>", maxKillsString)
        .placeholder("<broken_player_streak>", victimKs?.kills.toString())
        .placeholder("<broken_player_max_streak>", victimKs?.maxKills.toString())
        .papi(player)
        .format()
        .build()
        .message
    }



    // Send the max killstreak message if the player has reached their max killstreak
    if (kills == maxKills) {
      val message = defaultPlaceholders(conf.getString("maxKillstreak", "")!!)
      player.sendMessage(message)
    }

    // If "showKillstreak" is enabled, display it in the killer's actionbar
    if (conf.getBoolean("showKillstreak")) {
      val message = defaultPlaceholders(conf.getString("killstreakActionbar", "")!!)
      Utils.sendToActionBar(player, message, true)
    }

    // If a milestone is reached, handle it appropriately
    val milestone = plugin.milestones.firstOrNull { it.streak == kills }
    if (milestone != null) {
      val message = defaultPlaceholders(milestone.message ?: "")

      val commands = mutableListOf<String>()
      if (milestone.commands.isNotEmpty()) {
        milestone.commands.forEach {
          commands.add(defaultPlaceholders(it))
        }
      }

      handleMilestone(message, commands, milestone.effect, milestone.sound, player)
    }
    
    // If a killstreak was broken, find the according killstreak breaker
    // and handle it as well
    val brokenMilestone = findBrokenMilestone(victimKs)
    if (brokenMilestone != null) {
      val message = breakerPlaceholders(brokenMilestone.message ?: "")

      val commands = mutableListOf<String>()
      if (brokenMilestone.commands.isNotEmpty()) {
        brokenMilestone.commands.forEach {
          commands.add(breakerPlaceholders(it))
        }
      }

      handleMilestone(message, commands, brokenMilestone.effect, brokenMilestone.sound, player)
    }
  }

  private fun fetchLocalizedName(item: ItemStack?, def: String) : String {
    if (item == null) return def
    val meta = item.itemMeta ?: return def
    if (!meta.hasLocalizedName()) return def
    return meta.localizedName
  }
  
  private fun findBrokenMilestone(streak: Int) : Milestone? {
    // Iterate through all milestones in plugin.brokenMilestones
    // and find the milestone that corresponds to lowest
    // killstreak that is higher than the victim's killstreak
    var brokenMilestone: Milestone? = null
    plugin.brokenMilestones.forEach {
      println("Iterating through $it (streak: ${it.streak})")
      if (it.streak <= streak) {
        brokenMilestone = it
      }
    }

    return brokenMilestone
  }
  
  private fun findBrokenMilestone(streak: Killstreak?) : Milestone? {
    if (streak == null) return null
    return findBrokenMilestone(streak.kills)
  }

  private fun handleMilestone(message: String?, commands: List<String>?, effect: PotionEffect?, sound: Sound?, player: Player) {
    Bukkit.getOnlinePlayers().forEach {
      if (message != null) it.sendMessage(message)
    }

    commands?.forEach {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it)
    }

    if (effect != null) player.addPotionEffect(effect)
    if (sound != null) player.playSound(player.location, sound, 1f, 1f)
  }
}
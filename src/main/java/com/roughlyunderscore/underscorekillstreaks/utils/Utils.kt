package com.roughlyunderscore.underscorekillstreaks.utils

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Utils {
  companion object {
    fun format(message: String?) : String {
      return ChatColor.translateAlternateColorCodes('&', message!!)
    }

    fun sendToActionBar(pl: Player, message0: String?, toFormat: Boolean = false) {
      var message : String? = message0
      if (toFormat) message = format(message!!)
      pl.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(format(message!!)))
    }

    /**
     * Accepts a string; tries to fetch a player from UUID; if that fails, fetches a player from name
     */
    fun getOfflinePlayerFromString(string: String) : OfflinePlayer {
      return try {
        Bukkit.getOfflinePlayer(UUID.fromString(string))
      } catch (e: IllegalArgumentException) {
        Bukkit.getOfflinePlayer(string)
      }
    }

    private fun parseEffect(name: String?, duration: String?, amplifier: String?) : PotionEffect? {
      if (name == null || duration == null || amplifier == null) return null

      return try {
        PotionEffect(PotionEffectType.getByName(name)!!, duration.toInt(), amplifier.toInt())
      } catch (e: Exception) {
        null
      }
    }

    fun parseEffect(data: String?) : PotionEffect? {
      if (data == null) return null
      if (data.isEmpty() || data.isBlank()) return null

      val split = data.split(" ")
      if (split.size != 3) return null

      return parseEffect(split[0], split[1], split[2])
    }

    fun parseSound(name: String?) : Sound? {
      if (name == null) return null
      if (name.isEmpty() || name.isBlank()) return null

      return try {
        Sound.valueOf(name)
      } catch (e: Exception) {
        null
      }
    }

    fun clamp(min: Int, value: Int, max: Int) : Int {
      return min.coerceAtLeast(value.coerceAtMost(max))
    }
  }
}
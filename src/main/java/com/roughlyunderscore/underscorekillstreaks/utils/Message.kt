package com.roughlyunderscore.underscorekillstreaks.utils

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class Message private constructor(val message: String) {

  data class Builder(var message: String = "") {
    fun placeholder(placeholder: String, value: String) = apply { message = message.replace(placeholder, value) }
    fun papi(player: Player) = apply { this.message = PlaceholderAPI.setPlaceholders(player, message) }
    fun papi(uuid: UUID) = apply {
      val player = Bukkit.getOfflinePlayer(uuid)
      this.message = PlaceholderAPI.setPlaceholders(player, message)
    }
    fun format() = apply { message = Utils.format(message) }
    fun build() = Message(message)
  }

}
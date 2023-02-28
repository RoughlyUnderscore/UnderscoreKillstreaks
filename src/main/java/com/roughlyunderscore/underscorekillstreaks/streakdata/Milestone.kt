package com.roughlyunderscore.underscorekillstreaks.streakdata

import org.bukkit.Sound
import org.bukkit.potion.PotionEffect

class Milestone(val streak: Int, val message: String?, val sound: Sound?, val effect: PotionEffect?, val commands: List<String>) {
  override fun toString(): String {
    return "Milestone(streak=$streak, message=$message, sound=$sound, effect=$effect, commands=$commands)"
  }

  override fun hashCode(): Int {
    var result = streak
    result = 31 * result + (message?.hashCode() ?: 0)
    result = 31 * result + (sound?.hashCode() ?: 0)
    result = 31 * result + (effect?.hashCode() ?: 0)
    result = 31 * result + commands.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Milestone) return false

    if (streak != other.streak) return false
    if (message != other.message) return false
    if (sound != other.sound) return false
    if (effect != other.effect) return false
    if (commands != other.commands) return false

    return true
  }
}
package com.roughlyunderscore.underscorekillstreaks.streakdata

class Killstreak(var kills: Int, var maxKills: Int) {
  override fun toString(): String {
    return "Killstreak(kills=$kills, maxKills=$maxKills)"
  }

  override fun hashCode(): Int {
    var result = kills
    result = 31 * result + maxKills
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Killstreak) return false

    if (kills != other.kills) return false
    if (maxKills != other.maxKills) return false

    return true
  }
}
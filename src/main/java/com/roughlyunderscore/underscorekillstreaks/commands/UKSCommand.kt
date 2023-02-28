package com.roughlyunderscore.underscorekillstreaks.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.HelpCommand
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.roughlyunderscore.underscorekillstreaks.Constants
import com.roughlyunderscore.underscorekillstreaks.UnderscoreKillstreaks
import com.roughlyunderscore.underscorekillstreaks.tasks.LeaderboardUpdater
import com.roughlyunderscore.underscorekillstreaks.utils.Message
import com.roughlyunderscore.underscorekillstreaks.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

@CommandAlias("uks|underscorekillstreaks|killstreaks|ks|killstreak")
@Suppress("unused") // The methods are very much used and seeing the warnings is annoying
class UKSCommand() : BaseCommand() {
  private lateinit var plugin: UnderscoreKillstreaks
  private lateinit var conf: FileConfiguration
  constructor(plugin: UnderscoreKillstreaks) : this() {
    this.plugin = plugin
    conf = plugin.config
  }

  // Note to self: this works!
  @Default
  @HelpCommand
  @CatchUnknown
  @Subcommand("help")
  @Description("Shows the help message")
  @Syntax("")
  fun help(sender: CommandSender) {
    if (!evaluatePerms(sender, Constants.HELP_PERM)) return

    conf.getStringList("messages.help").forEach {
      sender.tell(Message.Builder(it))
    }
  }

  // Note to self: this works!
  @Subcommand("reload|rl|rel|refresh|ref|re|relo|reloa|r")
  @Description("Reloads the plugin")
  @Syntax("")
  fun reload(sender: CommandSender) {
    if (!evaluatePerms(sender, Constants.RELOAD_PERM)) return

    plugin.reloadPlugin()
    sender.tell(Message.Builder(conf.getString("messages.reload", "Could not find reload message in config")!!))
  }

  // Note to self: this works!
  @Subcommand("serialize|s|se|ser|seri|seria|serial|seriali|serializ")
  @Description("Serializes the killstreaks to a file/database")
  @Syntax("")
  fun serialize(sender: CommandSender) {
    if (!evaluatePerms(sender, Constants.SERIALIZE_PERM)) return

    plugin.serialize()
    sender.tell(Message.Builder(conf.getString("messages.serialized", "Could not find serialize message in config")!!))
  }

  // Note to self: this works!
  @Subcommand("deserialize|d|de|des|deser|deseri|deserial|deseriali|deserializ")
  @Description("Deserializes the killstreaks from a file/database")
  @Syntax("")
  fun deserialize(sender: CommandSender) {
    if (!evaluatePerms(sender, Constants.DESERIALIZE_PERM)) return

    plugin.deserialize()
    sender.tell(Message.Builder(conf.getString("messages.deserialized", "Could not find deserialize message in config")!!))
  }

  // Note to self: this works!
  @Subcommand("streak|streaks|k|ks|killstreak|killstreaks")
  @Description("Shows the streak of a player")
  @Syntax("<player>")
  @CommandCompletion("@players")
  fun streak(sender: CommandSender, playerID: String) {
    if (!evaluatePerms(sender, Constants.STREAK_PERM)) return

    val player = Utils.getOfflinePlayerFromString(playerID)
    val uuid = player.uniqueId
    val killstreak = UnderscoreKillstreaks.killstreaks[uuid]
    val kills = killstreak?.kills ?: 0
    val maxKills = killstreak?.maxKills ?: 0

    sender.tell(Message.Builder(conf.getString("messages.streak", "Could not find streak message in config")!!)
      .placeholder("<player>", player.name ?: "Unknown")
      .placeholder("<streak>", kills.toString())
      .placeholder("<max_streak>", maxKills.toString())
      .papi(uuid))
  }

  // Note to self: this works!
  @Subcommand("maxstreak|maxstreaks|maxkillstreak|maxkillstreaks|maximumstreak|maximumstreaks|maximumkillstreak|maximumkillstreaks")
  @Description("Shows the maximum streak of a player")
  @Syntax("<player>")
  @CommandCompletion("@players")
  fun maxStreak(sender: CommandSender, playerID: String) {
    if (!evaluatePerms(sender, Constants.MAXSTREAK_PERM)) return

    val player = Utils.getOfflinePlayerFromString(playerID)
    val uuid = player.uniqueId
    val killstreak = UnderscoreKillstreaks.killstreaks[uuid]
    val kills = killstreak?.kills ?: 0
    val maxKills = killstreak?.maxKills ?: 0

    sender.tell(Message.Builder(conf.getString("messages.max-streak", "Could not find max-streak message in config")!!)
      .placeholder("<player>", player.name ?: "Unknown")
      .placeholder("<streak>", kills.toString())
      .placeholder("<max_streak>", maxKills.toString())
      .papi(uuid))
  }

  @Subcommand("setstreak|setstreaks|setkillstreak|setkillstreaks")
  @Description("Sets the streak of a player")
  @Syntax("<player> <streak>")
  @CommandCompletion("@players @nothing")
  fun setStreak(sender: CommandSender, playerID: String, streak: Int) {
    if (!evaluatePerms(sender, Constants.SETSTREAK_PERM)) return

    val player = Utils.getOfflinePlayerFromString(playerID)
    val uuid = player.uniqueId
    val killstreak = UnderscoreKillstreaks.killstreaks[uuid]
    var maxKills = killstreak?.maxKills ?: 0
    killstreak?.kills = 0.coerceAtLeast(streak)
    // If the new streak is greater than the max streak, set the max streak to the new streak
    if ((killstreak?.kills ?: 0) > maxKills) {
      killstreak!!.maxKills = killstreak.kills
      maxKills = killstreak.maxKills
    }

    sender.tell(Message.Builder(conf.getString("messages.set-streak", "Could not find set-streak message in config")!!)
      .placeholder("<player>", player.name ?: "Unknown")
      .placeholder("<streak>", streak.toString())
      .placeholder("<max_streak>", maxKills.toString())
      .papi(uuid))
  }

  @Subcommand("setmaxstreak|setmaxstreaks|setmaxkillstreak|setmaxkillstreaks")
  @Description("Sets the maximum streak of a player")
  @Syntax("<player> <max_streak>")
  @CommandCompletion("@players @nothing")
  fun setMaxStreak(sender: CommandSender, playerID: String, maxStreak: Int) {
    if (!evaluatePerms(sender, Constants.SETMAXSTREAK_PERM)) return

    val player = Utils.getOfflinePlayerFromString(playerID)
    val uuid = player.uniqueId
    val killstreak = UnderscoreKillstreaks.killstreaks[uuid]
    val streak = killstreak?.kills ?: 0
    killstreak?.maxKills = 0.coerceAtLeast(maxStreak)

    sender.tell(Message.Builder(conf.getString("messages.set-max-streak", "Could not find set-max-streak message in config")!!)
      .placeholder("<player>", player.name ?: "Unknown")
      .placeholder("<streak>", streak.toString())
      .placeholder("<max_streak>", maxStreak.toString())
      .papi(uuid))
  }

  @Subcommand("topstreak|topstreaks|topkillstreak|topkillstreaks|lb|leaderboard|leaderboards")
  @Description("Shows the top streaks")
  @Syntax("<page>")
  @Conditions("player")
  fun topStreaks(sender: Player, @Optional page: Int?) {
    if (!evaluatePerms(sender, Constants.LEADERBOARD_PERM)) return

    val header = conf.getStringList("leaderboard.header")
    val footer = conf.getStringList("leaderboard.footer")
    val syntax = conf.getString("leaderboard.syntax") ?: "&6&l<position>. &d<player> &6&l- &d<streak>"
    val size = conf.getInt("leaderboard.size")
    val displaySelf = conf.getBoolean("leaderboard.displaySelf")
    val prefixBeforeSelf = conf.getStringList("leaderboard.prefixBeforeSelf")
    val selfStreak = conf.getString("leaderboard.selfStreak") ?: "&6&l%ukspos%. &d%player_name% &6&l- &d%uksmax%"

    val killstreaks = UnderscoreKillstreaks.killstreaks.toList().sortedByDescending { (_, value) -> value.maxKills }.toMap()
    val lines = mutableListOf<String>()

    val highestEverStreak = killstreaks.values.maxByOrNull { it.maxKills }?.maxKills ?: 0

    header.forEach {
      lines.add(Message.Builder(it)
        .placeholder("<max_streak_ever>", highestEverStreak.toString())
        .format()
        .papi(sender)
        .build()
        .message)
    }

    for (index in killstreaks.keys.indices) {
      if (killstreaks.size < index) break
      val player = killstreaks.keys.elementAt(index)
      val streak = killstreaks.values.elementAt(index).kills
      val maxStreak = killstreaks.values.elementAt(index).maxKills
      lines.add(Message.Builder(syntax)
        .placeholder("<position>", (index + 1).toString())
        .placeholder("<player>", Bukkit.getOfflinePlayer(player).name ?: "Unknown")
        .placeholder("<streak>", streak.toString())
        .placeholder("<max_streak>", maxStreak.toString())
        .placeholder("<max_streak_ever>", highestEverStreak.toString())
        .format()
        .papi(sender)
        .build()
        .message)
    }

    if (displaySelf) prefixBeforeSelf.forEach {
      lines.add(Message.Builder(it)
        .placeholder("<max_streak_ever>", highestEverStreak.toString())
        .papi(sender)
        .format()
        .build()
        .message
      )
    }
    lines.add(Message.Builder(selfStreak).papi(sender).format().build().message)

    footer.forEach {
      lines.add(Message.Builder(it)
        .placeholder("<max_streak_ever>", highestEverStreak.toString())
        .papi(sender)
        .format()
        .build()
        .message
      )
    }

    val pages = lines.chunked(size)
    val pageToDisplay = if (page == null || page < 1 || page > pages.size) 1 else page
    val pageLines = pages[pageToDisplay - 1]

    for (line in pageLines) {
      sender.tell(Message.Builder(line))
    }
  }

  @Subcommand("spawnleaderboard|spawnleaderboards|spawnlb|spawnlbs")
  @Description("Spawns a leaderboard")
  @Conditions("player")
  fun spawnLeaderboard(sender: Player) {
    if (!evaluatePerms(sender, Constants.SPAWNLEADERBOARD_PERM)) return

    val location = sender.location

    val latestID = conf.getInt("INTERNAL_FIELD_leaderboardLatestID") + 1
    conf.set("INTERNAL_FIELD_leaderboardLatestID", latestID)
    conf.set("INTERNAL_FIELD_leaderboards", conf.getStringList("INTERNAL_FIELD_leaderboards") + "leaderboard_underscore_killstreaks_$latestID")
    plugin.saveConfig()
    plugin.reloadConfig()

    val hologram = plugin.hologramManager.createLeaderboard(location, "leaderboard_underscore_killstreaks_$latestID")

    plugin.addRepeatingTask(LeaderboardUpdater(hologram, plugin.hologramManager))
  }


  private fun evaluatePerms(sender: CommandSender, permission: String): Boolean {
    return if (sender.hasPermission(permission)) true
    else {
      sender.sendMessage(Utils.format(
        conf.getString("messages.no-permission", "Could not find no-permission message in config")
      ))
      false
    }
  }

}

// Shorthand for sending a message
private fun CommandSender.tell(builder: Message.Builder) {
  sendMessage(builder.format().build().message)
}

private fun Player.tell(builder: Message.Builder) {
  sendMessage(builder.papi(this).format().build().message)
}
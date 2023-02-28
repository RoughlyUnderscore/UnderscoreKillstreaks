package com.roughlyunderscore.underscorekillstreaks

import co.aikar.commands.ConditionFailedException
import co.aikar.commands.PaperCommandManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jeff_media.updatechecker.UpdateCheckSource
import com.jeff_media.updatechecker.UpdateChecker
import com.roughlyunderscore.underscorekillstreaks.commands.UKSCommand
import com.roughlyunderscore.underscorekillstreaks.database.mongodb.MongoDB
import com.roughlyunderscore.underscorekillstreaks.database.mysql.MySQL
import com.roughlyunderscore.underscorekillstreaks.hologram.HologramManager
import com.roughlyunderscore.underscorekillstreaks.hook.PAPIUKS
import com.roughlyunderscore.underscorekillstreaks.hook.PAPIUKSMax
import com.roughlyunderscore.underscorekillstreaks.hook.PAPIUKSPos
import com.roughlyunderscore.underscorekillstreaks.listener.LeaveListener
import com.roughlyunderscore.underscorekillstreaks.listener.ManagementListener
import com.roughlyunderscore.underscorekillstreaks.listener.ServerLoadListener
import com.roughlyunderscore.underscorekillstreaks.streakdata.Killstreak
import com.roughlyunderscore.underscorekillstreaks.streakdata.Milestone
import com.roughlyunderscore.underscorekillstreaks.tasks.LeaderboardUpdater
import com.roughlyunderscore.underscorekillstreaks.utils.Utils
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.FileReader
import java.util.*

class UnderscoreKillstreaks : JavaPlugin() {
  companion object {
    var killstreaks: MutableMap<UUID, Killstreak> = HashMap()
  }

  private lateinit var serializer: String

  private lateinit var mySQLIP: String
  private var mySQLPort: Int = 3306
  private lateinit var mySQLDatabase: String
  private lateinit var mySQLUsername: String
  private lateinit var mySQLPassword: String
  private lateinit var mySQLTable: String

  private lateinit var mongoDBIP: String
  private lateinit var mongoDBDatabase: String
  private lateinit var mongoDBUsername: String
  private lateinit var mongoDBPassword: String
  private lateinit var mongoDBCollection: String

  private lateinit var mongoDB: MongoDB

  private lateinit var updateChecker: UpdateChecker

  private lateinit var uksCommand: UKSCommand
  private lateinit var commandManager: PaperCommandManager
  lateinit var hologramManager: HologramManager

  private lateinit var gson: Gson

  private val tasks: MutableMap<BukkitRunnable, Int> = mutableMapOf()

  val milestones = mutableListOf<Milestone>()
  val brokenMilestones = mutableListOf<Milestone>()
  val worldBlacklist = mutableListOf<World>()
  val playerBlacklist = mutableListOf<String>()

  override fun onEnable() {
    super.onEnable()

    init()

    // Deserialize all killstreaks
    deserialize()
  }

  override fun onDisable() {
    super.onDisable()

    serialize()
  }

  fun reloadPlugin() {
    serialize()
    HandlerList.unregisterAll(this)
    updateChecker.stop()
    commandManager.unregisterCommand(uksCommand)

    tasks.forEach { (task, _) -> task.cancel() }

    milestones.clear()
    brokenMilestones.clear()
    worldBlacklist.clear()
    playerBlacklist.clear()
    tasks.clear()



    reloadConfig()
    init()
    initTasks() // Called separately from init() because it can't be called in onEnable

    deserialize()
  }







  // Initializers

  private fun init() {
    initConfig()
    initMilestones()
    initLimiters()
    initSerializers()
    initBStats()
    initUpdateChecker()
    initListeners()
    initPAPI()
    initCommands()

    hologramManager = HologramManager(this)

    gson = GsonBuilder().setPrettyPrinting().create()

    tasks.forEach { (task, time) -> task.runTaskTimer(this, 0, time.toLong()) }
  }

  private fun initConfig() {
    // Header
    config.options().setHeader(listOf(
      "UnderscoreKillstreaks configuration",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "This is the main configuration file for UnderscoreKillstreaks.",
      "PLEASE read this header thoroughly before editing the configuration.",
      "Doing so will save you a lot of headaches.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "NOTE! This configuration is not safe to edit while the server is running",
      "and its contents may be overwritten by the plugin at any time.",
      "Even though \"/uks reload\" command exists, please discourage yourself from using it",
      "unless you 1000% know what you're doing. I am not responsible for any data loss.",
      "Please stop the server before editing this file.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "Please do not edit any fields that are prefixed with INTERNAL_FIELD.",
      "Doing so may cause the plugin to malfunction.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "If you need help, please join my Discord server: https://discord.gg/bBge7bj3ra",
      "Thank you for using UnderscoreKillstreaks v${description.version}!",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "bStats: Indicates whether bStats is enabled or not.",
      "Required value: boolean.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "updateChecker: Indicates whether updateChecker is enabled or not.",
      "Required value: boolean.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "updateCheckerHours: Indicates how much hours to wait between every update check.",
      "Required value: integer above 0.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "showKillstreak: Indicates whether to show the player their killstreak upon increasing or not.",
      "Required value: boolean.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "killstreakActionbar: Indicates what to show in the actionbar when the player's killstreak increases.",
      "Required value: string.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "hand: Indicates what to show in the killstreak message in <weapon>/<nc_weapon> when the player is using their hand.",
      "Required value: string.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "maxKillstreak: The message that gets sent to the player when they are at their highest ever killstreak.",
      "Required value: string.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "serializer: Indicates what serializer to use.",
      "Either put \"json\", \"mysql\" or \"mongodb\" (case-insensitive). Defaults to \"json\".",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "worldBlacklist: A list of worlds where the killstreaks will not be handled at all.",
      "Required value: string list.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "playerBlacklist: A list of players who won't be subject to any killstreak handling.",
      "Required value: string list.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "resetOnLeave: Indicates whether to reset the player's killstreak when they leave the server or not.",
      "Required value: boolean.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "LEADERBOARD SECTION TUTORIAL",
      "------------------------------------------",
      "Leaderboard is a feature that allows you to see the highest killstreak players in the server via a hologram.",
      "Using this feature requires DecentHolograms.",
      "------------------------------------------",
      "\"header\": The header of the leaderboard.",
      "Supports a placeholder \"<max_streak_ever>\" - returns all-time max killstreak",
      "Required value: string list.",
      "------------------------------------------",
      "\"footer\": The footer of the leaderboard.",
      "Supports a placeholder \"<max_streak_ever>\"",
      "Required value: string list.",
      "------------------------------------------",
      "\"syntax\" supports the following placeholders:",
      "<position> - returns the position of the player in the leaderboard",
      "<player> - returns player's name",
      "<streak> - returns player's killstreak",
      "<max_streak> - returns player's max killstreak",
      "<max_streak_ever>",
      "------------------------------------------",
      "displaySelf: Indicates whether to display the player's own killstreak in the hologram or not.",
      "Required value: boolean.",
      "------------------------------------------",
      "prefixBeforeSelf: Some lines after the top players in the leaderboard that will precede the player's own killstreak.",
      "This is only used if displaySelf is set to true.",
      "Required value: string list.",
      "------------------------------------------",
      "updateFrequency: The frequency of the leaderboard update in seconds.",
      "Required value: integer above 0.",
      "------------------------------------------",
      "size: The size of the leaderboard. Clamped between 1 and 15.",
      "Required value: integer between 1 and 15.",
      "------------------------------------------",
      "selfStreak: The line that will display the player's own killstreak.",
      "This is only used if displaySelf is set to true.",
      "This only supports PAPI placeholders.",
      "Required value: string.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "KILLSTREAKS SECTION TUTORIAL (killstreaks)",
      "This section does not exist by default. Create it",
      "and fill it how you want it to be.",
      "------------------------------------------",
      "Every entry has this syntax:",
      "5:                                         <- kills (required)",
      "  message: \"<player> is rocking!\"        <- message with placeholders supported (optional)",
      "  sound: \"BLOCK_ANVIL_USE\"               <- BLOCK_ANVIL_USE - name (optional)",
      "  effect: \"ABSORPTION 4 1\"               <- ABSORPTION - name, 4 - seconds, 1 - level (optional)",
      "  commands:                                <- commands to execute (optional)",
      "    - \"say <player> is rocking!\"",
      "------------------------------------------",
      "Usable message & command placeholders:",
      "<player> - returns player's name",
      "<weapon> - returns colored weapon name",
      "<nc_weapon> - returns not colored weapon name",
      "<streak> - returns killstreak",
      "<max_streak> - returns max killstreak",
      "------------------------------------------",
      "Usable sounds can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html",
      "If you don't want one, don't use this field.",
      "------------------------------------------",
      "Usable effects can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html",
      "If you don't want one, don't use this field.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "KILLSTREAK BREAKERS SECTION TUTORIAL (killstreakBreakers)",
      "This section does not exist by default. Create it",
      "and fill it how you want it to be.",
      "------------------------------------------",
      "Every entry has this syntax:",
      "5:                                                                       <- minimum kills (required)",
      "  message: \"<breaker_player> destroyed <broken_player>'s killstreak!\"  <- message with placeholders supported (optional)",
      "  sound: \"BLOCK_ANVIL_USE\"                                             <- BLOCK_ANVIL_USE - name (optional)",
      "  effect: \"ABSORPTION 4 1\"                                             <- ABSORPTION - name, 4 - seconds, 1 - level (optional)",
      "  commands:                                                              <- commands to execute (optional)",
      "    - \"say <broken_player> lost their killstreak!\"",
      "------------------------------------------",
      "Killstreak breakers work in the following way:",
      "Say there are two killstreak breakers registered with 5 and 10 kills respectively.",
      "When player A ends player B's killstreak of 4 or less, no killstreak breaker will be used.",
      "When player A ends player B's killstreak of 5-9 inclusive, the 5 killstreak breaker will be used.",
      "When player A ends player B's killstreak of 10+, the 10 killstreak breaker will be used.",
      "------------------------------------------",
      "Usable message & command placeholders:",
      "<breaker_player> - returns the name of the player who stopped the other player's killstreak",
      "<broken_player> - returns the name of the player whose killstreak was stopped",
      "<weapon> - returns colored weapon name",
      "<nc_weapon> - returns not colored weapon name",
      "<breaker_player_streak> - returns breaker's killstreak",
      "<breaker_player_max_streak> - returns breaker's max killstreak",
      "<broken_player_streak> - returns broken's streak (before it was broken)",
      "<broken_player_max_streak> - returns broken's max killstreak",
      "------------------------------------------",
      "Usable sounds can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html",
      "If you don't want one, don't use this field.",
      "------------------------------------------",
      "Usable effects can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html",
      "If you don't want one, don't use this field.",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "© 2023 RoughlyUnderscore with ❤"

    ))

    // Miscellaneous
    config.addDefault("bStats", true)
    config.addDefault("updateChecker", true)
    config.addDefault("updateCheckerHours", 12)

    // Limiters
    config.addDefault("worldBlacklist", listOf("blacklisted_world"))
    config.addDefault("playerBlacklist", listOf("Notch", "Dinnerbone"))

    config.addDefault("resetOnLeave", true)

    // Serialization
    config.addDefault("serializer", "json")
    config.addDefault("mysql.ip", "localhost")
    config.addDefault("mysql.port", 3306)
    config.addDefault("mysql.database", "killstreaks")
    config.addDefault("mysql.table", "killstreaks")
    config.addDefault("mysql.login", "root")
    config.addDefault("mysql.password", "password")
    config.addDefault("mongodb.ip", "localhost")
    config.addDefault("mongodb.database", "killstreaks")
    config.addDefault("mongodb.collection", "killstreaks")
    config.addDefault("mongodb.login", "root")
    config.addDefault("mongodb.password", "password")

    // Killstreak misc
    config.addDefault("killstreakActionbar", "&6Killstreak: &d<streak>/&7<max_streak>")
    config.addDefault("hand", "hand")
    config.addDefault("maxKillstreak", "&aYou are at your highest ever killstreak of &e<streak>!")
    config.addDefault("showKillstreak", true)

    // Leaderboard
    config.addDefault("leaderboard.header", listOf(
      "&6&lHighest killstreak leaderboard",
      "&6&l---------------------"
    ))
    config.addDefault("leaderboard.syntax", "&6&l<position>. &d<player> &6&l- &d<max_streak>")
    config.addDefault("leaderboard.footer", listOf(
      "&7&l---------------------",
      "&6&l<max_streak_ever> &6&lis the highest killstreak ever!",
      "&7&l---------------------"
    ))
    config.addDefault("leaderboard.size", 10)
    config.addDefault("leaderboard.displaySelf", true)
    config.addDefault("leaderboard.prefixBeforeSelf", listOf(
      "&6&l---------------------"
    ))
    config.addDefault("leaderboard.selfStreak", "&6&l%ukspos%. &d%player_name% &6&l- &d%uksmax%")
    config.addDefault("leaderboard.updateFrequency", 5)

    // Messages
    config.addDefault("messages.help", listOf(
      "&6&l---------------------",
      "&6&l/killstreaks help &7- &dShows this help menu.",
      "&6&l/killstreaks reload &7- &dReloads the plugin.",
      "&6&l/killstreaks serialize &7- &dForcefully loads all killstreaks to a file/database.",
      "&6&l/killstreaks deserialize &7- &dForcefully loads all killstreaks from a file/database.",
      "&6&l/killstreaks streak <player> &7- &dShows the player's killstreak.",
      "&6&l/killstreaks setstreak <player> <streak> &7- &dSets the player's killstreak to the specified streak.",
      "&6&l/killstreaks maxstreak <player> &7- &dShows the player's max killstreak.",
      "&6&l/killstreaks setmaxstreak <player> <streak> &7- &dSets the player's max killstreak to the specified streak.",
      "&6&l/killstreaks streak &7- &dShows your killstreak.",
      "&6&l/killstreaks setstreak <streak> &7- &dSets your killstreak to the specified streak.",
      "&6&l/killstreaks maxstreak &7- &dShows your max killstreak.",
      "&6&l/killstreaks setmaxstreak <streak> &7- &dSets your max killstreak to the specified streak.",
      "&6&l/killstreaks leaderboard [page] &7- &dShows the leaderboard",
      "&6&l/killstreaks spawnleaderboard &7- &dSpawns a hologram leaderboard via DecentHolograms",
      "&6&l---------------------",
    ))
    config.addDefault("messages.reload", "&aSuccessfully reloaded the plugin!")
    config.addDefault("messages.no-permission", "&cYou do not have enough permissions to do this!")
    config.addDefault("messages.serialized", "&aSuccessfully serialized all killstreaks!")
    config.addDefault("messages.deserialized", "&aSuccessfully deserialized all killstreaks!")
    config.addDefault("messages.streak", "&a<player>'s &6killstreak is &e<streak>")
    config.addDefault("messages.max-streak", "&a<player>'s &6max killstreak is &e<max_streak>")
    config.addDefault("messages.set-streak", "&aSuccessfully set &6<player>&a's killstreak to &e<streak>")
    config.addDefault("messages.set-max-streak", "&aSuccessfully set &6<player>&a's max killstreak to &e<max_streak>")
    config.addDefault("messages.spawned-leaderboard", "&aSuccessfully spawned the leaderboard!")

    config.options().copyDefaults(true)
    saveConfig()
  }

  private fun initMilestones() {
    val milestonesSection = config.getConfigurationSection("killstreaks")

    if (milestonesSection != null) {
      for (key in milestonesSection.getKeys(false)) {
        val currentMilestone = milestonesSection.getConfigurationSection(key) ?: continue

        val message = currentMilestone.getString("message", "")
        val effect = Utils.parseEffect(currentMilestone.getString("effect"))
        val sound = Utils.parseSound(currentMilestone.getString("sound"))
        val commands = currentMilestone.getStringList("commands")

        this.milestones.add(Milestone(key.toInt(), message, sound, effect, commands))
      }
    }

    val brokenMilestonesSection = config.getConfigurationSection("killstreakBreakers")

    if (brokenMilestonesSection != null) {
      for (key in brokenMilestonesSection.getKeys(false)) {
        val currentMilestone = brokenMilestonesSection.getConfigurationSection(key) ?: continue

        val message = currentMilestone.getString("message", "")
        val effect = Utils.parseEffect(currentMilestone.getString("effect"))
        val sound = Utils.parseSound(currentMilestone.getString("sound"))
        val commands = currentMilestone.getStringList("commands")

        this.brokenMilestones.add(Milestone(key.toInt(), message, sound, effect, commands))
      }
    }
  }

  private fun initLimiters() {
    config.getStringList("worldBlacklist").forEach {
      val world = Bukkit.getWorld(it)
      if (world != null) worldBlacklist.add(world)
    }
    config.getStringList("playerBlacklist").forEach { playerBlacklist.add(it) }
  }

  private fun initSerializers() {
    serializer = config.getString("serializer") ?: "json"

    mySQLIP = config.getString("mysql.ip") ?: "localhost"
    mySQLPort = config.getInt("mysql.port")
    mySQLDatabase = config.getString("mysql.database") ?: "killstreaks"
    mySQLUsername = config.getString("mysql.login") ?: "root"
    mySQLPassword = config.getString("mysql.password") ?: "password"
    mySQLTable = config.getString("mysql.table") ?: "killstreaks"

    mongoDBIP = config.getString("mongodb.ip") ?: "localhost"
    mongoDBDatabase = config.getString("mongodb.database") ?: "killstreaks"
    mongoDBUsername = config.getString("mongodb.login") ?: "root"
    mongoDBPassword = config.getString("mongodb.password") ?: "password"
    mongoDBCollection = config.getString("mongodb.collection") ?: "killstreaks"
  }

  private fun initBStats() {
    if (config.getBoolean("bStats")) Metrics(this, Constants.BSTATS_ID)
  }

  private fun initUpdateChecker() {
    if (config.getBoolean("updateChecker")) updateChecker = UpdateChecker(this, UpdateCheckSource.SPIGOT, Constants.SPIGOT_ID)
      .suppressUpToDateMessage(true)
      .checkEveryXHours(config.getInt("updateCheckerHours").toDouble())
      .checkNow()
  }

  private fun initListeners() {
    server.pluginManager.registerEvents(ManagementListener(this), this)
    server.pluginManager.registerEvents(LeaveListener(this), this)
    server.pluginManager.registerEvents(ServerLoadListener(this), this)
  }

  private fun initPAPI() {
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      PAPIUKS().register()
      PAPIUKSMax().register()
      PAPIUKSPos().register()
    }
  }

  private fun initCommands() {
    commandManager = PaperCommandManager(this)
    uksCommand = UKSCommand(this)

    commandManager.registerCommand(uksCommand)

    commandManager.commandConditions.addCondition("player") { context ->
      val sender = context.issuer
      if (sender.isPlayer) return@addCondition
      throw ConditionFailedException("This command can only be executed by a player")
    }
  }

  fun initTasks() {
    val leaderboards = config.getStringList("INTERNAL_FIELD_leaderboards")
    val hologramUpdateInterval = config.getInt("leaderboard.updateFrequency")

    println("All holograms: ${hologramManager.allHolograms()}")
    for (leaderboard in leaderboards) {
      println("> Leaderboard: $leaderboard")
      val hologram = hologramManager.getHologram(leaderboard) ?: continue
      println(">> Hologram: $hologram")
      val hologramUpdater = LeaderboardUpdater(hologram, hologramManager)
      addRepeatingTask(hologramUpdater, hologramUpdateInterval)
      println(">> Started updating every $hologramUpdateInterval ticks (${
        hologramUpdateInterval / 20
      } seconds)")
    }
  }

  fun addRepeatingTask(task: BukkitRunnable, ticks: Int = 100) {
    tasks[task] = ticks
    task.runTaskTimer(this, 1, ticks.toLong())
  }








  // Serializers

  fun deserialize() {
    if (serializer.equals("json", true)) deserializeJSON()
    if (serializer.equals("mysql", true)) deserializeMySQL()
    if (serializer.equals("mongodb", true)) deserializeMongoDB()
  }

  fun serialize() {
    if (serializer.equals("json", true)) serializeJSON()
    if (serializer.equals("mysql", true)) serializeMySQL()
    if (serializer.equals("mongodb", true)) serializeMongoDB()
  }

  private fun serializeJSON() {
    val killstreaksFile = File(dataFolder, "killstreaks.json")
    if (!killstreaksFile.exists()) killstreaksFile.createNewFile()

    val writer = killstreaksFile.writer()
    writer.use {
      gson.toJson(killstreaks, it)
      writer.flush()
    }
  }

  private fun deserializeJSON() {
    val killstreaksFile = File(dataFolder, "killstreaks.json")
    if (!killstreaksFile.exists()) killstreaksFile.createNewFile()
    else {
      val reader = FileReader(killstreaksFile)
      reader.use {
        val type = object : TypeToken<Map<UUID, Killstreak>>() {}.type
        killstreaks.putAll(gson.fromJson(it, type))
      }
    }
  }

  private fun serializeMySQL() {
    MySQL.loginQueryClose(mySQLIP, mySQLPort, mySQLDatabase, mySQLUsername, mySQLPassword, arrayOf(
      MySQL.CREATE_TABLE.replace("killstreaks", mySQLTable)
    ))
    killstreaks.forEach { (uuid, ks) ->
      MySQL.loginQueryClose(mySQLIP, mySQLPort, mySQLDatabase, mySQLUsername, mySQLPassword, arrayOf(
        MySQL.INSERT_KILLSTREAK
          .replace("<1>", uuid.toString())
          .replace("<2>", ks.kills.toString())
          .replace("<3>", ks.maxKills.toString())
          .replace("killstreaks", mySQLTable)
      ))
    }
  }

  private fun deserializeMySQL() {
    val set = MySQL.loginSelect(mySQLIP, mySQLPort, mySQLDatabase, mySQLUsername, mySQLPassword,
      MySQL.SELECT_ALL.replace("killstreaks", mySQLTable)
    )
    set.use {
      while (set.next()) {
        // Values: 0: uuid, 1: killstreak, 2: max
        val uuid = UUID.fromString(set.getString(0))
        val killstreak = set.getInt(1)
        val max = set.getInt(2)
        killstreaks[uuid] = Killstreak(killstreak, max)
      }
    }
  }

  private fun serializeMongoDB() {
    mongoDB.connect(mongoDBIP, mongoDBDatabase, mongoDBCollection, mongoDBUsername, mongoDBPassword)
    killstreaks.forEach { (uuid, ks) ->
      mongoDB.updateStreak(uuid.toString(), ks.kills, ks.maxKills)
    }
    mongoDB.disconnect()
  }

  private fun deserializeMongoDB() {
    mongoDB = MongoDB()
    mongoDB.connect(mongoDBIP, mongoDBDatabase, mongoDBCollection, mongoDBUsername, mongoDBPassword)
    mongoDB.fetchAllPlayerData().forEach {
      val uuid = UUID.fromString(it.getString("uuid"))
      val killstreak = it.getInteger("streak")
      val max = it.getInteger("maxstreak")
      killstreaks[uuid] = Killstreak(killstreak, max)
    }
    mongoDB.disconnect()
  }
}

// fixme Leaderboards don't update after restart
// fixme For some reason using /uks setstreak so that it overrides the max streak as well breaks the leaderboard


// todo API: PlayerKillstreakEndEvent (easy)
// todo API: PlayerKillstreakMilestoneEvent (easy)
// todo API: fetch player's killstreak (easy)
// todo API: change player's killstreak (easy)
// todo API: migrate Killstreak to API (easy)
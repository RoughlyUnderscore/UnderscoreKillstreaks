package com.roughlyunderscore.underscorekillstreaks.database.mysql

import java.sql.DriverManager
import java.sql.ResultSet

class MySQL private constructor() {
  init {
    Class.forName("com.mysql.cj.jdbc.Driver").newInstance()
  }
  companion object {
    /**
     * This performs three operations:<br>
     * 1) A MySQL database is accessed.<br>
     * 2) A SQL query (or multiple) (not SELECT) is executed.<br>
     * 3) The connection is closed.<br>
     * @param ip The IP of the MySQL server
     * @param port The port of the MySQL server
     * @param database The database to access
     * @param username The username to access the database
     * @param password The password to access the database
     * @param query The SQL query (queries) to execute
     */
    fun loginQueryClose(
      ip: String,
      port: Int,
      database: String,
      username: String,
      password: String,
      query: Array<String>
    ) {
      val url = "jdbc:mysql://$ip:$port/$database"
      val connection = DriverManager.getConnection(url, username, password)
      val statement = connection.createStatement()
      query.forEach { statement.executeUpdate(it) }
      connection.close()
    }

    /**
     * This performs two operations:<br>
     * 1) A MySQL database is accessed.<br>
     * 2) A SQL query (SELECT) is executed.<br>
     * @param ip The IP of the MySQL server
     * @param port The port of the MySQL server
     * @param database The database to access
     * @param username The username to access the database
     * @param password The password to access the database
     * @param query The SQL query to execute
     * @return The result of the query
     * @see ResultSet
     */
    fun loginSelect(
      ip: String,
      port: Int,
      database: String,
      username: String,
      password: String,
      query: String
    ): ResultSet {
      val url = "jdbc:mysql://$ip:$port/$database"
      val connection = DriverManager.getConnection(url, username, password)
      val statement = connection.prepareStatement(query)
      return statement.executeQuery()
    }

    /**
     * Creates a table.
     * Optional: killstreaks - table name
     */
    const val CREATE_TABLE = """
      CREATE TABLE IF NOT EXISTS killstreaks (
      `uuid` VARCHAR(36) NOT NULL,
      `streak` INT NOT NULL,
      `maxstreak` INT NOT NULL,
      UNIQUE (`uuid`),
      PRIMARY KEY (`uuid`)
      );
    """

    /**
     * Optional: killstreaks - table name
     */
    const val SELECT_ALL = "SELECT * FROM killstreaks;"

    /**
     * <1> = UUID<br>
     * <2> = streak<br>
     * <3> = maxstreak<br>
     * Optional: killstreaks - table name
     */
    const val INSERT_KILLSTREAK = """
      INSERT INTO killstreaks (`uuid`, `streak`, `maxstreak`) VALUES ('<1>', '<2>', '<3>')
      ON DUPLICATE KEY UPDATE streak = '<2>', maxstreak = '<3>';
    """
  }
}
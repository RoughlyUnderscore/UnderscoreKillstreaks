package com.roughlyunderscore.underscorekillstreaks.database.mongodb

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document

class MongoDB {
  private lateinit var client: MongoClient
  private lateinit var db: MongoDatabase
  private lateinit var collection: MongoCollection<Document>

  fun connect(ip: String, dbName: String, collectionName: String, username: String, password: String) {
    try {
      client = MongoClients.create("mongodb+srv://$username:$password@$ip/$dbName?retryWrites=true&w=majority")
      db = client.getDatabase(dbName)
      collection = db.getCollection(collectionName)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun fetchPlayerData(uuid: String): Document {
    return collection.find(Document("uuid", uuid)).first() ?: Document()
  }

  fun fetchAllPlayerData(): List<Document> {
    return collection.find().toList()
  }

  fun updateStreak(uuid: String, streak: Int, maxstreak: Int) {
    if (collection.find(Filters.eq("uuid", uuid)).first() == null) {
      collection.insertOne(Document("uuid", uuid).append("streak", streak).append("maxstreak", maxstreak))
    } else {
      collection.updateOne(Filters.eq("uuid", uuid), Updates.set("streak", streak))
      collection.updateOne(Filters.eq("uuid", uuid), Updates.set("maxstreak", maxstreak))
    }
  }

  fun disconnect() {
    client.close()
  }
}
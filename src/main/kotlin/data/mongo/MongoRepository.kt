package org.tumba.gollum.data

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase

abstract class MongoRepository<T>(mongoClient: MongoClient, dbName: String) {
    protected val db: MongoDatabase = mongoClient.getDatabase(dbName)
}
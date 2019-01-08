package org.tumba.gollum

import com.fasterxml.jackson.annotation.JsonInclude
import com.mongodb.MongoClientOptions
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.litote.kmongo.KMongo
import org.tumba.gollum.data.mongo.MongoAccountRepository
import java.io.File


fun main(args: Array<String>) {
    val port = 9000
    val importTimeoutMs = 1000 * 60 * 5
    val dbHostname = "localhost"
    val dbName = "contest"

    val dataPath = "/tmp/data/data.zip"
    val optionsPath = "/tmp/data/options.txt"
    val optionsLines = File(optionsPath).readLines()
    val optionsNow = optionsLines[0].trim().toLong()

    val mongoClientOptionsBuilder = MongoClientOptions.Builder()
    mongoClientOptionsBuilder.maxConnectionIdleTime(importTimeoutMs)
    val importMongoClient = KMongo.createClient(dbHostname, mongoClientOptionsBuilder.build())
    val importRepository = MongoAccountRepository(importMongoClient, dbName, optionsNow)
    importRepository.createIndexes()

    val dataImporter = DataImporter(importRepository, dataPath)
    dataImporter.import()

    val mongoClient = KMongo.createClient(dbHostname)
    val accountRepository = MongoAccountRepository(mongoClient, dbName, optionsNow)
    val routes = Routes(accountRepository)

    embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            jackson {
                this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
        routing { routes.getRoute(this) }
    }.start(wait = true)
}
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


fun main(args: Array<String>) {
    val mongoClient1Options = MongoClientOptions.Builder()
    mongoClient1Options.maxConnectionIdleTime(1000 * 60 * 2)

    val mongoClient1 = KMongo.createClient("localhost", mongoClient1Options.build())
    val dataImporter = DataImporter(MongoAccountRepository(mongoClient1, "contest"))
    dataImporter.import()

    val mongoClient = KMongo.createClient("localhost")
    val accountRepository = MongoAccountRepository(mongoClient, "contest")
    println("Current account repository: " + accountRepository.size())

    embeddedServer(Netty, 80) {
        install(ContentNegotiation) {
            jackson {
                this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
        routing { Routes(accountRepository).getRoute(this) }
    }.start(wait = true)
}
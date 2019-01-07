package org.tumba.gollum

import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.litote.kmongo.KMongo
import org.tumba.gollum.data.mongo.MongoAccountRepository


fun main(args: Array<String>) {
    val mongoClient = KMongo.createClient("localhost")
    val accountRepository = MongoAccountRepository(mongoClient, "contest")
    val dataImporter = DataImporter(accountRepository)
    dataImporter.import()

    println("Current account repository: " + accountRepository.size())

    embeddedServer(Netty, 80) {
        install(ContentNegotiation) {
            jackson {
            }
        }
        routing { Routes(accountRepository).getRoute(this) }
    }.start(wait = true)
}
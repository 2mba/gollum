package org.tumba.gollum

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val accountRepository = InMemoryAccountRepository()
    val dataImporter = DataImporter(accountRepository)

    dataImporter.import()

    println("Current account repository: " + accountRepository.size())

    embeddedServer(Netty, 8080) {
        routing { Routes().getRoute(this) }
    }.start(wait = true)
}
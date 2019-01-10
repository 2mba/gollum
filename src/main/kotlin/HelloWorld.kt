package org.tumba.gollum

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {
    val port = 80
    val dbHostname = "localhost"
    val dbName = "contest"

    val dataPath = "data/data.zip"//"C:\\temp\\data.zip"//"/tmp/data/data.zip"
    //val optionsPath = "/tmp/data/options.txt"//"C:\\temp\\options.txt" //"/tmp/data/options.txt"
    //val optionsLines = File(optionsPath).readLines()
    //val optionsNow = optionsLines[0].trim().toLong()

    val importRepository = Factories.accountRepositoryFactory.getAccountRepository()

    val dataImporter = DataImporter(importRepository, dataPath)
    measureTimeMillis {
        dataImporter.import()
    }.also { println("Import $it ms") }


    val accountRepository = Factories.accountRepositoryFactory.getAccountRepository()
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
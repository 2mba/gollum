package org.tumba.gollum

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty


fun main(args: Array<String>) {
    val port = 80
    val repository = AccountRepository()
    val dataPath = "/tmp/data/data.zip"//"C:\\temp\\data.zip"//"/tmp/data/data.zip"
    val dataImporter = DataImporter(repository, dataPath)
    dataImporter.import()

    val routes = Routes(repository)

    embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            jackson {
                this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
        routing { routes.getRoute(this) }
    }.start(wait = true)
}
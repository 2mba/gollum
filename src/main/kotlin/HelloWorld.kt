package org.tumba.gollum

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    embeddedServer(Netty, 8080) {
        routing {
            get("/") {
                call.respondText(
                    text = "Hello, World",
                    contentType = ContentType.Text.Html,
                    status = HttpStatusCode.OK
                )
            }
        }
    }.start(wait = true)
}
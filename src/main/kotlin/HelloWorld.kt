package org.tumba.gollum

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val reader = DataReader()
    reader.test();

    embeddedServer(Netty, 8080) {
        routing { Routes().getRoute(this) }
    }.start(wait = true)
}
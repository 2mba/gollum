package org.tumba.gollum

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext

class Routes {

    fun getRoute(routing: Routing) {
        routing.route("accounts") {
            get("filter") {
                notImplemented()
            }
            get("group") {
                notImplemented()
            }
            route("{id}") {
                get("recommend") {
                    notImplemented()
                }
                get("suggest") {
                    notImplemented()
                }
                post("/") {
                    notImplemented()
                }
            }
            post("new") {
                notImplemented()
            }
            post("likes") {
                notImplemented()
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.notImplemented() {
    call.respond("Not implemented: ${this.call.request.path()}")
}
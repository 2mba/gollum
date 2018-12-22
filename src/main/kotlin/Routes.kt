package org.tumba.gollum

import domain.repository.FieldCondition
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.filter
import io.ktor.util.flattenEntries
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import org.tumba.gollum.data.mongo.MongoAccountRepository

class Routes(private val repository: MongoAccountRepository) {
    fun getRoute(routing: Routing) {
        routing.route("accounts") {
            get("filter") {
                val queryParams = context.request.queryParameters
                    .filter { k, _ -> k != "limit" && k != "query_id" }
                    .flattenEntries()
                val map: List<FieldCondition>

                try {
                    map = queryParams
                        .map {
                            val keyParts = it.first.split('_')
                            FieldCondition(keyParts[0], keyParts[1], it.second)
                        }
                }
                catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val limitStr = context.request.queryParameters["limit"]
                val limit = limitStr!!.toInt()

                val accounts = repository.filter(map, limit)
                call.respond(accounts)

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
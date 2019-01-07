package org.tumba.gollum

import domain.repository.FieldCondition
import domain.repository.validate
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.filter
import io.ktor.util.flattenEntries
import io.ktor.util.pipeline.PipelineContext
import org.tumba.gollum.data.mongo.MongoAccountRepository
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountList


class Routes(private val repository: MongoAccountRepository) {
    fun getRoute(routing: Routing) {
        routing.route("accounts") {
            get("filter") {
                val queryParams = context.request.queryParameters
                    .filter { param, _ -> param != "limit" && param != "query_id" }
                    .flattenEntries()

                val fieldConditions: List<FieldCondition>

                try {
                    fieldConditions = queryParams.map {
                        val keyParts = it.first.split('_')
                        if (keyParts.size != 2 || keyParts[0].isEmpty() || keyParts[1].isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "{}")
                            return@get
                        }
                        val condition = FieldCondition(keyParts[0], keyParts[1], it.second)
                        if (!condition.validate()) {
                            call.respond(HttpStatusCode.BadRequest, "{}")
                            return@get
                        }
                        condition
                    }
                }
                catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@get
                }

                val limitStr = context.request.queryParameters["limit"]
                val limit = limitStr!!.toInt()

                val accounts = repository.filter(fieldConditions, limit)
                call.respond(AccountList(accounts))
            }
//            get("group") {
//                notImplemented()
//            }
//            route("{id}") {
//                get("recommend") {
//                    notImplemented()
//                }
//                get("suggest") {
//                    notImplemented()
//                }
//                post("/") {
//                    notImplemented()
//                }
//            }
            post("new") {
                val account = call.receive<Account>()
                // TODO: validation, conflict, etc
                repository.insert(account)
                call.respond(HttpStatusCode.Created, "{}")
            }
//            post("likes") {
//                notImplemented()
//            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.notImplemented() {
    call.respond("Not implemented: ${this.call.request.path()}")
}
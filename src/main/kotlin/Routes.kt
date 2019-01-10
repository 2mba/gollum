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
import org.tumba.gollum.domain.entities.*


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
            post("new") {
                val account: Account

                try {
                    account = call.receive<Account>()
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }
                if (!account.validate()) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (!repository.insert(account)) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                call.respond(HttpStatusCode.Created, "{}")
                return@post
            }
            post("{id}") {
                val idStr = call.parameters["id"]
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }
                val id = idStr.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.NotFound, "{}")
                    return@post
                }

                val accountPatch: AccountPatch

                try {
                    accountPatch = call.receive<AccountPatch>()
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (!accountPatch.validate()) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                try {
                    if (!repository.update(id, accountPatch)) {
                        call.respond(HttpStatusCode.NotFound, "{}")
                        return@post
                    }
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                call.respond(HttpStatusCode.Accepted, "{}")
                return@post
            }
            post("likes") {
                val likeInfoList: LikeInfoList

                try {
                    likeInfoList = call.receive<LikeInfoList>()
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (likeInfoList.likes.isEmpty()) {
                    call.respond(HttpStatusCode.Accepted, "{}")
                    return@post
                }

//                if (!repository.updateLikes(likeInfoList.likes)) {
//                    call.respond(HttpStatusCode.BadRequest, "{}")
//                    return@post
//                }

                call.respond(HttpStatusCode.Accepted, "{}")
                return@post
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.notImplemented() {
    call.respond("Not implemented: ${this.call.request.path()}")
}
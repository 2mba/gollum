package org.tumba.gollum

import com.dslplatform.json.DslJson
import domain.FieldCondition
import domain.validate
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.filter
import io.ktor.util.flattenEntries
import org.tumba.gollum.domain.entities.*
import org.tumba.gollum.domain.repository.IAccountRepository


class Routes(
    private val repository: IAccountRepository,
    private val dslJson: DslJson<Any>
) {
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
                } catch (e: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@get
                }

                val limitStr = context.request.queryParameters["limit"]
                val limit = limitStr!!.toInt()

                var accounts = repository.filter(fieldConditions, limit)

                val jsonWriter = dslJson.newWriter()
                dslJson.serialize(jsonWriter, AccountList(accounts))
                call.respond(jsonWriter.toString())
            }

            get("group") {
                val groupQueryParams = context.request.queryParameters
                    .filter { param, _ -> param != "limit" && param != "query_id" && param != "keys" && param != "order" }
                    .flattenEntries()

                val conditions: List<FieldCondition>

                try {
                    conditions = groupQueryParams.map {
                        val predicate = if (it.first == "birth" || it.first == "joint") "year" else "eq"
                        val condition = FieldCondition(it.first, predicate, it.second)
                        if (!condition.validate()) {
                            call.respond(HttpStatusCode.BadRequest, "{}")
                            return@get
                        }
                        condition
                    }
                } catch (e: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@get
                }

                try {
                    val limitStr = context.request.queryParameters["limit"]
                    val limit = limitStr!!.toInt()

                    val orderStr = context.request.queryParameters["order"]
                    val order = orderStr!!.toInt()

                    val keyStr = context.request.queryParameters["keys"]
                    val keys = keyStr!!.split(',')

                    keys.forEach { k ->
                        if (!k.validateKey()) {
                            call.respond(HttpStatusCode.BadRequest, "{}")
                            return@get
                        }
                    }

                    var groups = repository.group(keys, conditions, limit, order)

                    val jsonWriter = dslJson.newWriter()
                    dslJson.serialize(jsonWriter, GroupList(groups))
                    call.respond(jsonWriter.toString())
                }
                catch (e: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@get
                }
            }

            post("new") {
                val account: Account

                try {
                    account = dslJson.deserialize(Account::class.java, call.receiveStream())
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }
                if (!account.validate()) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

//                if (!repository.insert(account)) {
//                    call.respond(HttpStatusCode.BadRequest, "{}")
//                    return@post
//                }

                call.respond(HttpStatusCode.Created, "{}")
                return@post
            }

            post("{id}") {
                val idStr = call.parameters["id"]
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }
                val id = idStr.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.NotFound, "{}")
                    return@post
                }

                val accountPatch: AccountPatch

                try {
                    accountPatch = dslJson.deserialize(AccountPatch::class.java, call.receiveStream())
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (!accountPatch.validate()) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                try {
//                    if (!repository.update(id, accountPatch)) {
//                        call.respond(HttpStatusCode.NotFound, "{}")
//                        return@post
//                    }
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
                    likeInfoList = dslJson.deserialize(LikeInfoList::class.java, call.receiveStream())
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (likeInfoList.likes.isEmpty()) {
                    call.respond(HttpStatusCode.Accepted, "{}")
                    return@post
                }

                // TODO: insert likes

                call.respond(HttpStatusCode.Accepted, "{}")
                return@post
            }

        }
    }
}
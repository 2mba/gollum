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


class InMemoryRepository {
    private val ids = HashMap<Int, String>()
    private val emails = HashSet<String>()

    fun tryInsert(account: Account): Boolean {
        synchronized(this) {
            if (ids.containsKey(account.id)) return false
            if (emails.contains(account.email)) return false
            ids[account.id] = account.email
            emails.add(account.email)
            return true
        }
    }

    fun tryUpdate(id: Int, account: AccountPatch): Boolean {
        synchronized(this) {
            if (!ids.containsKey(id)) return false
            if (account.email != null) {
                if (emails.contains(account.email))
                    throw IllegalArgumentException()
                emails.remove(ids[id])
                ids[id] = account.email
                emails.add(account.email)
            }
            return true
        }
    }
}

class Routes(
    private val repository: IAccountRepository,
    private val inMemoryRepository: InMemoryRepository,
    private val dslJson: DslJson<Any>
) {
    @Volatile var post: Boolean = false

    fun getRoute(routing: Routing) {
        routing.route("accounts") {
            get("filter") {
                if (post) {
                    call.respond(HttpStatusCode.TooManyRequests, "{}")
                    return@get
                }

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
                if (post) {
                    call.respond(HttpStatusCode.TooManyRequests, "{}")
                    return@get
                }
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
                post = true

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

                if (!inMemoryRepository.tryInsert(account)) {
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
                    if (!inMemoryRepository.tryUpdate(id, accountPatch)) {
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
                    likeInfoList = dslJson.deserialize(LikeInfoList::class.java, call.receiveStream())
                } catch (ex: Throwable) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (likeInfoList.likes.isEmpty()) {
                    call.respond(HttpStatusCode.Accepted, "{}")
                    return@post
                }

                call.respond(HttpStatusCode.Accepted, "{}")
                return@post
            }

        }
    }
}
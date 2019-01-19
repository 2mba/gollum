package org.tumba.gollum

import com.dslplatform.json.DslJson
import domain.FieldCondition
import domain.validate
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.ratelimits.rateLimit
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

class InterestsRepository {
    private val map = HashMap<String, HashSet<Long>>()

    fun insert(id: Long, interests: ArrayList<String>) {
        interests.forEach {
            if (map.containsKey(it)) {
                map[it]!!.add(id)
            }
            else {
                map[it] = hashSetOf(id)
            }
        }
    }

    fun contains(interests: List<String>): List<Long> {
        val counters = HashMap<Long, Int>()
        interests.forEach { interest ->
            if (map.containsKey(interest)) {
                map[interest]!!.forEach { id ->
                    counters[id] = counters.getOrDefault(id, 0) + 1
                }
            }
        }
        return counters
            .filter { c -> c.value == interests.size }
            .map { c -> c.key }
    }

    fun any(interests: List<String>): List<Long> {
        val counters = HashSet<Long>()
        interests.forEach { interest ->
            if (map.containsKey(interest)) {
                map[interest]!!.forEach { id ->
                    counters.add(id)
                }
            }
        }
        return counters.toList()
    }
}

class InMemoryRepository {
    private val interestsRepository: InterestsRepository = InterestsRepository()
    private val ids = HashMap<Long, String>()
    private val emails = HashSet<String>()

    fun tryInsert(account: Account): Boolean {
        synchronized(this) {
            if (ids.containsKey(account.id)) return false
            if (emails.contains(account.email)) return false
            ids[account.id] = account.email
            emails.add(account.email)
            if (account.interests != null)
                interestsRepository.insert(account.id, account.interests)
            return true
        }
    }

    fun tryUpdate(id: Long, account: AccountPatch): Boolean {
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

    fun filterInterests(predicate: String, interests: List<String>, limit: Int): List<Long> {
        val ids = if (predicate == "contains")
            interestsRepository.contains(interests)
        else
            interestsRepository.any(interests)

        return ids
            .sortedByDescending { it }
            .drop(limit)
    }
}

class Routes(
    private val repository: IAccountRepository,
    private val inMemoryRepository: InMemoryRepository,
    private val dslJson: DslJson<Any>
) {
    fun getRoute(routing: Routing) {
        routing.route("accounts") {
            rateLimit("filter", limit = 10, seconds = 5) {
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
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "{}")
                        return@get
                    }

                    val limitStr = context.request.queryParameters["limit"]
                    val limit = limitStr!!.toInt()

                    val interestsCondition = fieldConditions.firstOrNull { x -> x.fieldName == "interests" }
                    val idsFilteredByInterests: List<Long>
                    if (interestsCondition != null) {
                        idsFilteredByInterests = inMemoryRepository.filterInterests(
                            interestsCondition.predicate,
                            interestsCondition.value.split(','),
                            limit
                        )
                        if (idsFilteredByInterests.isEmpty()) {
                            val jsonWriter = dslJson.newWriter()
                            dslJson.serialize(jsonWriter, AccountList(arrayListOf()))
                            call.respond(jsonWriter.toString())
                            return@get
                        }
                    } else {
                        idsFilteredByInterests = arrayListOf()
                    }
                    val minInterestsId = idsFilteredByInterests.lastOrNull()
                    val maxInterestsId = idsFilteredByInterests.firstOrNull()

                    val accounts = repository.filter(fieldConditions, limit, minInterestsId, maxInterestsId)

                    val jsonWriter = dslJson.newWriter()
                    dslJson.serialize(jsonWriter, AccountList(accounts))
                    call.respond(jsonWriter.toString())
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
                val id = idStr.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
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
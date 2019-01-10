package org.tumba.gollum

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import org.tumba.gollum.domain.entities.*
import java.util.*
import kotlin.collections.HashSet

class AccountRepository {
    private val ids = HashMap<Long, String>()
    private val emails = HashSet<String>()

    fun tryInsert(account: Account): Boolean {
        synchronized(this) {
            if (ids.containsKey(account.id)) return false
            if (emails.contains(account.email)) return false
            //        if (account.likes != null){
            //            if (account.likes.any { !ids.containsKey(it.id) }) {
            //                return false
            //            }
            //        }

            ids[account.id] = account.email
            emails.add(account.email)
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

    fun tryUpdateLikes(likes: List<LikeInfo>): Boolean {
        return true
//        synchronized(this) {
//            likes.forEach { like ->
//                if (!ids.containsKey(like.likee) || ids.containsKey(like.liker)) {
//                    return false
//                }
//            }
//            return true
//        }
    }
}

class Routes(private val repository: AccountRepository) {
    fun getRoute(routing: Routing) {
        routing.route("accounts") {
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

                if (!repository.tryInsert(account)) {
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
                    call.respond(HttpStatusCode.NotFound)
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
                    if (!repository.tryUpdate(id, accountPatch)) {
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
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                if (!repository.tryUpdateLikes(likeInfoList.likes)) {
                    call.respond(HttpStatusCode.BadRequest, "{}")
                    return@post
                }

                call.respond(HttpStatusCode.Accepted, "{}")
                return@post
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.notImplemented() {
    call.respond("Not implemented: ${this.call.request.path()}")
}
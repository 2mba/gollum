package org.tumba.gollum.domain.entities

import com.dslplatform.json.CompiledJson

@CompiledJson
data class Premium(
    val start: Long,
    val finish: Long
)

@CompiledJson
data class Like(
    val id: Long,
    val ts: Long
)

@CompiledJson
data class Account(
    val id: Long,
    val fname: String?,
    val sname: String?,
    val email: String?,
    val interests: ArrayList<String>?,
    val status: String?,
    val premium: Premium?,
    val sex: String?,
    val phone: String?,
    val likes: ArrayList<Like>?,
    val birth: Long?,
    val city: String?,
    val country: String?,
    val joined: Long?
)
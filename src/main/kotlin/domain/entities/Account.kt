package org.tumba.gollum.domain.entities

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonValue

@CompiledJson
enum class Status(@get:JsonValue val value: String) {
    FREE("свободны"),
    BUSY("заняты"),
    COMPLICATED("всё сложно")
}

@CompiledJson
data class Premium(
    val start: Long,
    val end: Long
)

@CompiledJson
enum class Sex(@get:JsonValue val value: String) {
    MALE("m"),
    FEMALE("f")
}

@CompiledJson
data class Like(
    val id: Long,
    val timestamp: Long
)

@CompiledJson
data class Account(
    val id: Long,
    val firstName: String?,
    val surName: String?,
    val email: String,
    val interests: Array<String>?,
    val status: Status,
    val premium: Premium?,
    val sex: Sex,
    val phone: String?,
    val likes: Array<Like>?,
    val birth: Long,
    val city: String?,
    val country: String?,
    val joined: Long
)
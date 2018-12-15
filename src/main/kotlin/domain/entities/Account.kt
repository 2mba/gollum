package org.tumba.gollum.domain.entities

class Account(
    val id: Long,
    val firstName: String?,
    val surName: String?,
    val email: String,
    val interests: Array<String>,
    val status: Status,
    val premium: Premium,
    val sex: Sex,
    val phone: String?,
    val likes: Array<Like>,
    val birth: Long,
    val city: String?,
    val country: String?,
    val joined: Long
) {

    enum class Status {
        FREE,
        BUSY,
        COMPLICATED
    }

    class Premium(
        val start: Long,
        val end: Long
    )

    enum class Sex {
        MALE,
        FEMALE
    }

    class Like(
        id: Long,
        timestamp: Long
    )
}
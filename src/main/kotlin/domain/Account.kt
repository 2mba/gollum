package org.tumba.gollum.domain.entities

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonValue
import java.time.LocalDate

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
enum class Status(@get:JsonValue val value: String) {
    FREE("свободны"),
    BUSY("заняты"),
    COMPLICATED("всё сложно")
}


@CompiledJson
enum class Sex(@get:JsonValue val value: String) {
    MALE("m"),
    FEMALE("f")
}

@CompiledJson
data class Account(
    val id: Int,
    val email: String,
    val fname: String?,
    val sname: String?,
    val phone: String?,
    val sex: Sex?,
    val birth: Long?,
    val country: String?,
    val city: String?,
    val joined: Long?,
    val status: Status?,
    val interests: ArrayList<String>?,
    val premium: Premium?,
    val likes: ArrayList<Like>?
)

@CompiledJson
data class AccountList(
    val accounts: List<Account>
)

@CompiledJson
data class Group(
    val sex: String?,
    val status: String?,
    val country: String?,
    val city: String?,
    val count: Int
)

@CompiledJson
data class GroupList(
    val groups: List<Group>
)

@CompiledJson
data class LikeInfo(
    val liker: Long,
    val likee: Long,
    val ts: Long
)

@CompiledJson
data class LikeInfoList(
    val likes: List<LikeInfo>
)

@CompiledJson
data class AccountPatch(
    val email: String?,
    val fname: String?,
    val sname: String?,
    val phone: String?,
    val sex: String?,
    val birth: Long?,
    val country: String?,
    val city: String?,
    val joined: Long?,
    val status: String?,
    val interests: ArrayList<String>?,
    val premium: Premium?,
    val likes: ArrayList<Like>?
)

val minBirth = LocalDate.of(1950, 1, 1).toEpochDay() * 60 * 60 * 24
val maxBirth = LocalDate.of(2005, 1, 1).toEpochDay() * 60 * 60 * 24
val minJoined = LocalDate.of(2011, 1, 1).toEpochDay() * 60 * 60 * 24
val maxJoined = LocalDate.of(2018, 1, 1).toEpochDay() * 60 * 60 * 24
val minPremium = LocalDate.of(2018, 1, 1).toEpochDay() * 60 * 60 * 24

fun Account.validate(): Boolean {
    if (email.isEmpty() || email.length > 100 || !email.contains('@')) return false
    if (fname != null && (fname.isEmpty() || fname.length > 50)) return false
    if (sname != null && (sname.isEmpty() || sname.length > 50)) return false
    if (phone != null && (phone.isEmpty() || phone.length > 16)) return false
    if (sex == null) return false
    //if (sex.value != "m" && sex.value != "f") return false

    if (birth == null || birth < minBirth || birth > maxBirth) return false
    if (country != null && (country.isEmpty() || country.length > 50)) return false
    if (city != null && (city.isEmpty() || city.length > 50)) return false

    if (joined == null || joined < minJoined || joined > maxJoined) return false
    if (status == null) return false
    //if (status.value != "свободны" && status.value != "заняты" && status.value != "всё сложно") return false

    if (interests != null && interests.any { it.isEmpty() || it.length > 100 }) return false

    if (premium != null && premium.start < minPremium) return false

    // todo: likes
    return true
}

fun AccountPatch.validate(): Boolean {
    if (email != null)
        if (email.isEmpty() || email.length > 100 || !email.contains('@')) return false

    if (fname != null && (fname.isEmpty() || fname.length > 50)) return false
    if (sname != null && (sname.isEmpty() || sname.length > 50)) return false
    if (phone != null && (phone.isEmpty() || phone.length > 16)) return false
    if (sex != null)
        if (sex != "m" && sex != "f") return false

    if (birth != null)
        if (birth < minBirth || birth > maxBirth) return false

    if (country != null && (country.isEmpty() || country.length > 50)) return false
    if (city != null && (city.isEmpty() || city.length > 50)) return false

    if (joined != null)
        if (joined < minJoined || joined > maxJoined) return false
    if (status != null)
        if (status != "свободны" && status != "заняты" && status != "всё сложно") return false

    if (interests != null && interests.any { it.isEmpty() || it.length > 100 }) return false

    if (premium != null && premium.start < minPremium) return false


    // todo: likes
    return true
}

fun String.validateKey(): Boolean {
    // todo: move to static
    return arrayListOf("sex", "status", "country", "city").contains(this)
}
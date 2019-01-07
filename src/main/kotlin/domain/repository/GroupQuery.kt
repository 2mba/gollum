package org.tumba.gollum.domain.repository

data class Field(
    val name: String,
    val value: String
)

data class GroupQuery(
    val keys: List<String>,
    val fields: List<Field>
)

data class AccountGroup(
    val count: Int,
    val fname: String? = null,
    val sname: String? = null,
    val email: String? = null,
    val interests: String? = null,
    val status: String? = null,
    val sex: String? = null,
    val phone: String? = null,
    val likes: Long? = null,
    val birth: Long? = null, // year
    val city: String? = null,
    val country: String? = null,
    val joined: Long? = null // year
)
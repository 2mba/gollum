package org.tumba.gollum.data.sql

import domain.FieldCondition
import org.tumba.gollum.domain.entities.*
import org.tumba.gollum.domain.repository.IAccountRepository
import java.time.LocalDate
import kotlin.math.min

@ExperimentalUnsignedTypes
class MemoryRepository(private val now: Long) : IAccountRepository {

    private val accounts = Array<AccountEntity?>(1_340_000) { null }
    private val countries: MutableList<String> = ArrayList()
    private val cities: MutableList<String> = ArrayList()
    private val countriesMap: MutableMap<String, Int> = HashMap()
    private val citiesMap: MutableMap<String, Int> = HashMap()
    private val domains: MutableMap<String, Int> = HashMap()
    private var domainsSize = 0
    private val interestsMap: MutableMap<String, Int> = HashMap()
    private val interests: MutableList<String> = ArrayList()

    private var lastId = 0

    override fun insert(accounts: List<Account>) {
        accounts.forEach { insert(it) }
    }

    override fun insert(account: Account): Boolean {
        val id = account.id
        if (id !in accounts.indices) {
            println("Insert: out of range: $id")
            return false
        }
        if (accounts[id] != null) {
            return false
        }
        accounts[id] = account.toEntity()
        if (lastId < id) {
            lastId = id
        }
        return true
    }

    override fun update(id: Int, accountPatch: AccountPatch): Boolean {
        if (id !in accounts.indices) {
            println("Insert: out of range: $id")
            return false
        }
        val entity = accounts[id] ?: return false

        if (accountPatch.email != null) {
            entity.email = accountPatch.email
        }
        if (accountPatch.fname != null) {
            entity.fname = accountPatch.fname
        }
        if (accountPatch.sname != null) {
            entity.sname = accountPatch.sname
        }
        if (accountPatch.sex != null) {
            entity.sex = accountPatch.sex.toSexEntityValue()
        }
        if (accountPatch.country != null) {
            entity.country = getCountryId(accountPatch.country)
        }
        if (accountPatch.city != null) {
            entity.city = getCityId(accountPatch.city)
        }
        if (accountPatch.joined != null) {
            entity.joined = accountPatch.joined
        }
        if (accountPatch.status != null) {
            entity.status = accountPatch.status.toStatusEntityValue()
        }
        /*if (accountPatch.interests != null) {
            entity.interests = accountPatch.interests
        }*/
        /*if (accountPatch.likes != null) {
            entity.likes = accountPatch.likes
        }*/
        if (accountPatch.premium != null) {
            entity.premiumStart = accountPatch.premium.start
            entity.premiumFinish = accountPatch.premium.finish
        }
        if (accountPatch.birth != null) {
            entity.birth = accountPatch.birth
        }
        return true
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        val fields = hashSetOf<String>().apply {
            conditions.forEach { add(it.fieldName) }
        }
        return accounts
            .reversedIterator(lastId)
            .asSequence()
            .filter { entity ->
                entity?.filter(conditions) ?: false
            }
            .take(limit)
            .map { it!!.toAccount(fields) }
            .toList()
    }

    override fun group(keys: List<String>, conditions: List<FieldCondition>, limit: Int, order: Int): List<Group> {
        return listOf()
    }

    private fun Account.toEntity(): AccountEntity {
        val account = this
        val domain = getEmailDomain(account.email)
        val interestBitsets = account.interests?.let { getInterests(it) } ?: 0L to 0L
        return AccountEntity(
            id = account.id,
            email = account.email.intern(),
            domain = domain,
            fname = account.fname?.intern(),
            sname = account.sname?.intern(),
            phone = account.phone?.intern(),
            code = account.phone?.substringAfter("(")?.substringBefore(")")?.toShortOrNull(),
            sex = account.sex?.toEntity()!!,
            birth = account.birth,
            country = account.country?.let { getCountryId(it) },
            city = account.city?.let { getCityId(it) },
            joined = account.joined,
            status = account.status?.toEntity()!!,
            premiumStart = account.premium?.start,
            premiumFinish = account.premium?.finish,
            interestsBitset1 = interestBitsets.first,
            interestsBitset2 = interestBitsets.second
        )
    }

    private fun AccountEntity.toAccount(fields: Set<String>): Account {
        val entity = this
        return Account(
            id = entity.id,
            email = entity.email,
            fname = entity.fname.filterField("fname", fields),
            sname = entity.sname.filterField("sname", fields),
            phone = entity.phone.filterField("phone", fields),
            sex = entity.sex.toSex().filterField("sex", fields),
            birth = entity.birth.filterField("birth", fields),
            country = entity.country?.let { countries[it] }.filterField("country", fields),
            city = entity.city?.let { cities[it] }.filterField("city", fields),
            joined = entity.joined.filterField("joined", fields),
            status = entity.status.toStatus().filterField("status", fields),
            interests = null,
            premium = entity.premiumStart?.let {
                Premium(
                    entity.premiumStart!!,
                    entity.premiumFinish!!
                )
            }.filterField("premium", fields),
            likes = arrayListOf<Like>().filterField("likes", fields)
        )
    }

    private fun <T> T?.filterField(name: String, fields: Set<String>): T? {
        return if (fields.contains(name)) this else null
    }

    private fun getCountryId(country: String): Int {
        val id = countriesMap.getOrDefault(country, -1)
        return if (id >= 0) {
            id
        } else {
            val newId = countries.size
            countries.add(country)
            countriesMap[country] = newId
            newId
        }
    }

    private fun getCityId(city: String): Int {
        val id = citiesMap.getOrDefault(city, -1)
        return if (id >= 0) {
            id
        } else {
            val newId = cities.size
            cities.add(city)
            citiesMap[city] = newId
            newId
        }
    }

    private fun getInterestId(interest: String, addIfUnknown: Boolean = true): Int {
        val id = interestsMap.getOrDefault(interest, -1)
        return if (id >= 0) {
            id
        } else {
            if (addIfUnknown) {
                val newId = interests.size
                if (newId > 127) {
                    println("Can't add interest: $interest, size = $newId")
                    -1
                } else {
                    interests.add(interest)
                    interestsMap[interest] = newId
                    newId
                }
            } else {
                -1
            }
        }
    }

    private fun getInterests(interests: List<String>, addUnknownInterests: Boolean = true): Pair<Long, Long>? {
        var bitset1 = 0L
        var bitset2 = 0L
        interests
            .asSequence()
            .map { getInterestId(it, addUnknownInterests) }
            .forEach { id ->
                if (id < 0) {
                    if (addUnknownInterests) {
                        return null
                    } else {
                        return@forEach
                    }
                }
                if (id <= 63) {
                    bitset1 = bitset1.or(1L.shl(id))
                } else {
                    bitset2 = bitset2.or(1L.shl(id - 64))
                }
            }
        return bitset1 to bitset2
    }

    private fun AccountEntity.filter(conditions: List<FieldCondition>): Boolean {
        var res = true
        conditions.forEach { res = res && filter(it) }
        return res
    }

    private fun AccountEntity.filter(condition: FieldCondition): Boolean {
        return when (condition.fieldName) {
            "email" -> filterEmail(email, domain, condition.predicate, condition.value)
            "fname" -> filterString(fname, condition.predicate, condition.value)
            "sname" -> filterString(sname, condition.predicate, condition.value)
            "sex" -> filterInt(sex, condition.predicate, condition.value.toSexEntityValue())
            "status" -> filterInt(status, condition.predicate, condition.value.toStatusEntityValue())
            "country" -> filterCountry(country, condition.predicate, condition.value)
            "city" -> filterCity(city, condition.predicate, condition.value)
            "birth" -> filterBirth(birth, condition.predicate, condition.value)
            "premium" -> filterPremium(premiumStart, premiumFinish, condition.predicate, condition.value)
            "phone" -> filterPhone(phone, code, condition.predicate, condition.value)
            "interests" -> filterInterests(interestsBitset1, interestsBitset2, condition.predicate, condition.value)
            else -> true
        }
    }

    private fun filterEmail(email: String, domain: Int, predicate: String, filterValue: String): Boolean {
        return when (predicate) {
            "domain" -> {
                val id = domains.getOrDefault(filterValue, -1)
                if (id < 0) {
                    false
                } else {
                    domain == id
                }
            }
            "gt" -> email > filterValue
            "lt" -> email < filterValue
            else -> true
        }
    }

    private fun filterString(value: String?, predicate: String, filterValue: String): Boolean {
        return when (predicate) {
            "eq" -> value == filterValue
            "gt" -> if (value != null) value > filterValue else false
            "lt" -> if (value != null) value < filterValue else false
            "starts" -> value?.startsWith(filterValue) ?: false
            "null" -> if (filterValue == "0") value != null else value == null
            else -> true
        }
    }

    private fun filterCountry(country: Int?, predicate: String, filterValue: String): Boolean {
        val id = countriesMap.getOrDefault(filterValue, -1)
        return when (predicate) {
            "eq" -> if (id >= 0) country == id else false
            "null" -> if (filterValue == "0") country != null else country == null
            else -> true
        }
    }

    private fun filterCity(city: Int?, predicate: String, filterValue: String): Boolean {
        return when (predicate) {
            "any" -> {
                if (city == null) {
                    false
                } else {
                    filterValue
                        .split(',')
                        .asSequence()
                        .map { citiesMap.getOrDefault(it, -1) }
                        .filter { it >= 0 }
                        .any { city == it }
                }
            }
            "eq" -> {
                if (city == null) {
                    false
                } else {
                    val id = citiesMap.getOrDefault(filterValue, -1)
                    if (id >= 0) city == id else false
                }
            }
            "null" -> {
                if (filterValue == "0") city != null else city == null
            }
            else -> true
        }
    }

    private fun filterBirth(birth: Long?, predicate: String, filterValue: String): Boolean {
        if (birth == null) return false
        return when (predicate) {
            "eq" -> birth == filterValue.toLong()
            "gt" -> birth > filterValue.toLong()
            "lt" -> birth < filterValue.toLong()
            "year" -> {
                val year = filterValue.toInt()
                val yearStart = LocalDate.of(year, 1, 1).toEpochDay() * 60 * 60 * 24
                val yearEnd = LocalDate.of(year + 1, 1, 1).toEpochDay() * 60 * 60 * 24
                birth in yearStart..yearEnd
            }
            else -> true
        }
    }

    private fun filterPremium(
        premiumStart: Long?,
        premiumFinish: Long?,
        predicate: String,
        filterValue: String
    ): Boolean {
        return when (predicate) {
            "null" -> if (filterValue == "0") premiumStart != null else premiumStart == null
            "now" -> {
                if (null == premiumStart || null == premiumFinish)
                    false
                else
                    now in premiumStart..premiumFinish
            }
            else -> true
        }
    }

    private fun filterInt(value: Int, predicate: String, filterValue: Int): Boolean {
        return when (predicate) {
            "eq" -> value == filterValue
            "gt" -> value > filterValue
            "lt" -> value < filterValue
            "neq" -> value != filterValue
            else -> true
        }
    }

    private fun filterPhone(phone: String?, code: Short?, predicate: String, filterValue: String): Boolean {
        return when (predicate) {
            "code" -> filterValue.toShortOrNull()?.equals(code) ?: false
            "null" -> if (filterValue == "0") phone != null else phone == null
            else -> true
        }
    }

    private fun filterInterests(bitset1: Long, bitset2: Long, predicate: String, filterValue: String): Boolean {
        val interestsInput = filterValue.split(',')
        val checkBitset = getInterests(interestsInput, addUnknownInterests = false)!!
        return when (predicate) {
            "contains" -> {
                val res1 = bitset1.and(checkBitset.first) == checkBitset.first
                val res2 = bitset2.and(checkBitset.second) == checkBitset.second
                return res1 && res2
            }
            "any" -> {
                return bitset1.and(checkBitset.first) > 0 ||
                    bitset2.and(checkBitset.second) > 0
            }
            else -> true
        }
    }

    private fun getEmailDomain(email: String): Int {
        val domain = email.substring(email.indexOf('@') + 1)
        val id = domains.getOrDefault(domain, -1)
        return if (id < 0) {
            domains[domain] = domainsSize
            domainsSize++
        } else {
            id
        }
    }
}

private fun Sex.toEntity(): Int {
    return this.ordinal
}

private fun Status.toEntity(): Int {
    return this.ordinal
}

private fun Int.toSex(): Sex? {
    if (this < 0) return null
    return Sex.values()[this]
}

private fun Int.toStatus(): Status? {
    if (this < 0) return null
    return Status.values()[this]
}

private fun String.toSexEntityValue(): Int {
    return when (this) {
        "m" -> 0
        "f" -> 1
        else -> 0
    }
}

private fun String.toStatusEntityValue(): Int {
    return Status.values().first { it.value == this }.ordinal
}

typealias Id = Int

private class AccountEntity(
    var id: Id,
    var email: String,
    var domain: Int,
    var fname: String? = null,
    var sname: String? = null,
    var phone: String? = null,
    var code: Short? = null,
    var sex: Int,
    var birth: Long? = null,
    var country: Int? = null,
    var city: Int? = null,
    var joined: Long? = null,
    var status: Int,
    var interestsBitset1: Long,
    var interestsBitset2: Long,
    var premiumStart: Long? = null,
    var premiumFinish: Long? = null
)

private fun <T> Array<T>.reversedIterator(lastId: Int): Iterator<T> = ReverseIterator(this, lastId)

class ReverseIterator<T>(
    private val list: Array<T>,
    lastId: Int
) : Iterator<T>, Iterable<T> {
    private var position: Int = 0

    init {
        this.position = min(lastId, list.size - 1)
    }

    override fun iterator(): Iterator<T> {
        return this
    }

    override fun hasNext(): Boolean {
        return position >= 0
    }

    override fun next(): T {
        return list[position--]
    }
}
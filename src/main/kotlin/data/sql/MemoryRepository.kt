package org.tumba.gollum.data.sql

import domain.FieldCondition
import org.tumba.gollum.domain.entities.*
import org.tumba.gollum.domain.repository.IAccountRepository
import java.time.LocalDate

class MemoryRepository : IAccountRepository {

    private val accounts: MutableList<AccountEntity> = ArrayList(1_300_000)
    private val countries: MutableList<String> = ArrayList()
    private val cities: MutableList<String> = ArrayList()
    private val countriesMap: MutableMap<String, Int> = HashMap()
    private val citiesMap: MutableMap<String, Int> = HashMap()
    private val domains: MutableMap<String, Int> = HashMap()
    private var domainsSize = 0

    override fun insert(accounts: List<Account>) {
        accounts.forEach { insert(it) }
    }

    override fun insert(account: Account): Boolean {
        accounts.add(account.toEntity())
        return true
    }

    override fun update(id: Int, accountPatch: AccountPatch): Boolean {
        return true
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        return accounts
            .reversedIterator()
            .asSequence()
            .filter { entity ->
                entity.filter(conditions)
            }
            .take(limit)
            .map { it.toAccount() }
            .toList()
    }

    override fun group(keys: List<String>, conditions: List<FieldCondition>, limit: Int, order: Int): List<Group> {
        return listOf()
    }

    private fun Account.toEntity(): AccountEntity {
        val account = this
        val domain = getEmailDomain(account.email)
        return AccountEntity(
            id = account.id,
            email = account.email.intern(),
            domain = domain,
            fname = account.fname?.intern(),
            sname = account.sname?.intern(),
            phone = account.phone?.intern(),
            sex = account.sex?.toEntity()!!,
            birth = account.birth,
            country = account.country?.let { getCountryId(it) },
            city = account.city?.let { getCityId(it) },
            joined = account.joined,
            status = account.status?.toEntity()!!,
            interests = arrayOf(),
            premiumStart = account.premium?.start,
            premiumFinish = account.premium?.finish,
            likes = arrayOf()
        )
    }

    private fun AccountEntity.toAccount(): Account {
        val entity = this
        return Account(
            id = entity.id,
            email = entity.email,
            fname = entity.fname,
            sname = entity.sname,
            phone = entity.phone,
            sex = entity.sex.toSex(),
            birth = entity.birth,
            country = entity.country?.let { countries[it] },
            city = entity.city?.let { cities[it] },
            joined = entity.joined,
            status = entity.status.toStatus(),
            interests = arrayListOf(),
            premium = entity.premiumStart?.let { Premium(entity.premiumStart, entity.premiumFinish!!) },
            likes = arrayListOf()
        )
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

    private fun AccountEntity.filter(conditions: List<FieldCondition>): Boolean {
        var res = true
        conditions.forEach { res = res && filter(it) }
        return res
    }

    private fun AccountEntity.filter(condition: FieldCondition): Boolean {
        return when (condition.fieldName) {
            "email" -> filterEmail(email, domain, condition.predicate, condition.value)
            "fname" -> filterString(fname, condition.predicate, condition.value)
            "sname"-> filterString(sname, condition.predicate, condition.value)
            "phone"-> filterString(phone, condition.predicate, condition.value)
            "sex" -> filterInt(sex, condition.predicate, condition.value.toSexEntityValue())
            "status" -> filterInt(status, condition.predicate, condition.value.toStatusEntityValue())
            "country"-> filterCountry(country, condition.predicate, condition.value)
            "city"-> filterCity(city, condition.predicate, condition.value)
            "birth"-> filterBirth(birth, condition.predicate, condition.value)
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
            "gt" -> email < filterValue
            "lt" -> email > filterValue
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

    private fun filterInt(value: Int, predicate: String, filterValue: Int): Boolean {
        return when (predicate) {
            "eq" -> value == filterValue
            "gt" -> value > filterValue
            "lt" -> value < filterValue
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

private fun String.toSexEntityValue(): Int{
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
    val id: Id,
    val email: String,
    val domain: Int,
    val fname: String? = null,
    val sname: String? = null,
    val phone: String? = null,
    val sex: Int,
    val birth: Long? = null,
    val country: Int? = null,
    val city: Int? = null,
    val joined: Long? = null,
    val status: Int,
    val interests: Array<Int>? = null,
    val premiumStart: Long? = null,
    val premiumFinish: Long? = null,
    val likes: Array<Like>? = null
)

private fun <T> List<T>.reversedIterator(): Iterator<T> = ReverseIterator(this)

class ReverseIterator<T>(private val list: List<T>) : Iterator<T>, Iterable<T> {
    private var position: Int = 0

    init {
        this.position = list.size - 1
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

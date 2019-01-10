package org.tumba.gollum.data.mongo

import com.mongodb.MongoClient
import com.mongodb.MongoWriteException
import com.mongodb.client.model.*
import domain.repository.FieldCondition
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOneById
import org.tumba.gollum.data.MongoRepository
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountPatch
import org.tumba.gollum.domain.entities.LikeInfo
import org.tumba.gollum.domain.repository.AccountGroup
import org.tumba.gollum.domain.repository.GroupQuery
import org.tumba.gollum.domain.repository.IAccountRepository
import java.time.LocalDate

class MongoAccountRepository(mongoClient: MongoClient, dbName: String, private val now: Long) : IAccountRepository,
    MongoRepository<Account>(mongoClient, dbName) {
    override fun updateLikes(likes: List<LikeInfo>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(id: Long, accountPatch: AccountPatch): Boolean {
        val update = ArrayList<Bson>()
        if (accountPatch.email != null) {
            update.add(Updates.set("email", accountPatch.email))
        }
        if (accountPatch.birth != null) {
            update.add(Updates.set("birth", accountPatch.birth))
        }
        if (accountPatch.city != null) {
            update.add(Updates.set("city", accountPatch.city))
        }
        if (accountPatch.country != null) {
            update.add(Updates.set("country", accountPatch.country))
        }
        if (accountPatch.fname != null) {
            update.add(Updates.set("fname", accountPatch.fname))
        }
        if (accountPatch.sname != null) {
            update.add(Updates.set("sname", accountPatch.sname))
        }
        if (accountPatch.interests != null) {
            update.add(Updates.set("interests", accountPatch.interests))
        }
        if (accountPatch.joined != null) {
            update.add(Updates.set("joined", accountPatch.joined))
        }
        if (accountPatch.likes != null) {
            update.add(Updates.set("likes", accountPatch.likes))
        }
        if (accountPatch.phone != null) {
            update.add(Updates.set("phone", accountPatch.phone))
        }
        if (accountPatch.premium != null) {
            update.add(Updates.set("premium", accountPatch.premium))
        }
        if (accountPatch.sex != null) {
            update.add(Updates.set("sex", accountPatch.sex))
        }
        if (accountPatch.status != null) {
            update.add(Updates.set("status", accountPatch.status))
        }
        val updateDocument = Updates.combine(update)
        val updateOptions = UpdateOptions().upsert(false)
        val updateResult = db.getCollection<Account>().updateOneById(id, updateDocument, updateOptions)

        if (updateResult.matchedCount == 0L) {
            return false
        }

        if (updateResult.modifiedCount == 0L) {
            throw IllegalArgumentException()
        }

        return true
    }

    fun createIndexes() {
        db.getCollection<Account>()
            .ensureIndex(Document("email", 1), IndexOptions().unique(true))
//        db.getCollection<Account>()
//            .ensureIndex(Document("phone", 1), IndexOptions().unique(true).sparse(true))
    }

    override fun group(query: GroupQuery, limit: Int, order: Int): List<AccountGroup> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun size(): Int {
        return db.getCollection<Account>()
            .countDocuments()
            .toInt()
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        val filters = conditions.map { mapFieldCondition(it) }
        var projectionFields = conditions
            .asSequence()
            .map { c -> c.fieldName }
            .plus(arrayListOf("email", "id"))
            .distinct()
            .minus(arrayListOf("interests", "likes"))
            .toList()

        if (filters.any()) {
            return db.getCollection<Account>()
                .find(Filters.and(filters))
                .projection(Projections.include(projectionFields))
                .sort(Sorts.descending("_id"))
                .limit(limit)
                .toList()
        }

        return db.getCollection<Account>()
            .find()
            .projection(Projections.include(projectionFields))
            .sort(Sorts.descending("_id"))
            .limit(limit)
            .toList()
    }

    override fun insert(accounts: List<Account>) {
        db.getCollection<Account>()
            .insertMany(accounts)

    }

    override fun insert(account: Account): Boolean {
        try {
            db.getCollection<Account>().insertOne(account)
            return true
        } catch (ex: MongoWriteException) {
            return false
        }
    }

    private fun mapFieldCondition(condition: FieldCondition): Bson {
        // todo:
        when (condition.predicate) {
            "eq" -> {
                return Filters.eq(condition.fieldName, condition.value)
            }
            "neq" -> {
                return Filters.ne(condition.fieldName, condition.value)
            }
            "lt" -> {
                val number = condition.value.toLongOrNull()
                if (number != null)
                    return Filters.lt(condition.fieldName, number)
                return Filters.lt(condition.fieldName, condition.value)
            }
            "gt" -> {
                val number = condition.value.toLongOrNull()
                if (number != null)
                    return Filters.gt(condition.fieldName, number)
                return Filters.gt(condition.fieldName, condition.value)
            }
            "year" -> {
                val year = condition.value.toInt()
                val yearStart = LocalDate.of(year, 1, 1).toEpochDay() * 60 * 60 * 24
                val yearEnd = LocalDate.of(year+1, 1, 1).toEpochDay()* 60 * 60 * 24

                return Filters.and(
                    Filters.gte(condition.fieldName, yearStart),
                    Filters.lt(condition.fieldName, yearEnd))
            }
            "null" -> {
                val exists = condition.value == "0"
                return Filters.exists(condition.fieldName, exists)
            }
            "starts" -> {
                return Filters.regex(condition.fieldName, "^${condition.value}")
            }
            "code" -> {
                return Filters.regex(condition.fieldName, "\\(${condition.value}\\)")
            }
            "any" -> {
                val values = condition.value.split(',')
                val joint = values.joinToString { "\"$it\"" }
                if (condition.fieldName == "interests")
                    return Filters.and(
                        Filters.exists(condition.fieldName),
                        Filters.elemMatch(condition.fieldName, Document.parse("{ \"\$in\":[${joint}]}"))
                    )

                return Filters.`in`(condition.fieldName, values)
            }
            "contains" -> {
                val values = condition.value.split(',')
                if (condition.fieldName == "likes") {
                    return Filters.all("${condition.fieldName}._id", values.map { it.toInt() })
                }
                return Filters.all(condition.fieldName, values)
            }
            "domain" -> {
                return Filters.regex(condition.fieldName, condition.value + "$")
            }
            "now" -> {
                return Filters.and(
                    Filters.exists(condition.fieldName),
                    Filters.lte("${condition.fieldName}.start", now),
                    Filters.gte("${condition.fieldName}.finish", now)
                )
            }
        }

        throw IllegalArgumentException(condition.predicate)
    }
}
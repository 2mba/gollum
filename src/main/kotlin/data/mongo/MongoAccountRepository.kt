package org.tumba.gollum.data.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import domain.repository.FieldCondition
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.getCollection
import org.tumba.gollum.data.MongoRepository
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.repository.IAccountRepository
import java.time.LocalDate

class MongoAccountRepository(mongoClient: MongoClient, dbName: String) : IAccountRepository, MongoRepository<Account>(mongoClient, dbName) {
    override fun size(): Int {
        return db.getCollection<Account>()
            .countDocuments()
            .toInt()
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        val filters = conditions.map { mapFieldCondition(it) }
        if (filters.any()) {
            return db.getCollection<Account>()
                .find(Filters.and(filters))
                .projection(Projections.exclude("interests", "likes"))
                .limit(limit)
                .toList()
        }

        return db.getCollection<Account>()
            .find()
            .limit(limit)
            .projection(Projections.exclude("interests", "likes"))
            .toList()
    }

    override fun insert(accounts: List<Account>) {
        db.getCollection<Account>()
            .insertMany(accounts)

    }

    override fun insert(account: Account) {
        db.getCollection<Account>()
            .insertOne(account)
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
                    return Filters.elemMatch(condition.fieldName, Document.parse("{ \"\$in\":[${joint}]}"))
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
                val number = condition.value.toLong()
                return Filters.and(
                    Filters.gt("${condition.fieldName}.start", number),
                    Filters.lt("${condition.fieldName}.finish", number)
                )
            }
        }

        throw IllegalArgumentException(condition.predicate)
    }
}
package org.tumba.gollum.data.mongo

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.litote.kmongo.getCollection
import org.tumba.gollum.data.MongoRepository
import org.tumba.gollum.domain.entities.Account
import domain.repository.FieldCondition
import org.bson.Document
import org.tumba.gollum.domain.repository.IAccountRepository

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
                .limit(limit)
                .toList()
        }

        return db.getCollection<Account>()
            .find()
            .limit(limit)
            .toList()
    }

    override fun insert(accounts: List<Account>) {
        db.getCollection<Account>()
            .insertMany(accounts)

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
                return Filters.lt(condition.fieldName, condition.value)
            }
            "gt" -> {
                return Filters.gt(condition.fieldName, condition.value)
            }
            "null" -> {
                val exists = condition.value == "0"
                return Filters.exists(condition.fieldName, exists)
            }
            "starts" -> {
                return Filters.regex(condition.fieldName, "^${condition.value}")
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
                return Filters.all(condition.fieldName, values)
            }
            "domain" -> {
                return Filters.regex(condition.fieldName, condition.value + "$")
            }
        }

        throw IllegalArgumentException(condition.predicate)
    }
}
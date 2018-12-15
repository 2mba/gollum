package org.tumba.gollum.domain.repository

import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.Sex
import org.tumba.gollum.domain.entities.Status

interface IAccountRepository {

    fun insert(vararg accounts: Account)

    fun filter(query: FilterQuery): List<Account>

    data class FilterQuery(
        val sex: Sex? = null,
        val email: String? = null,
        val status: FieldCondition<Status>
        // todo
    )

}

sealed class FieldCondition<T> {

    class Equal<T>(value: T): FieldCondition<T>()

    class NotEqual<T>(value: T): FieldCondition<T>()

    class LessThen<T>(value: T): FieldCondition<T>()

    class GreaterThen<T>(value: T): FieldCondition<T>()

    class Any<T>(values: List<T>): FieldCondition<T>()

    class Contains<T>(values: List<T>): FieldCondition<T>()

}
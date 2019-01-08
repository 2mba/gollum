package org.tumba.gollum.domain.repository

import domain.repository.FieldCondition
import org.tumba.gollum.domain.entities.Account

interface IAccountRepository {
    fun size(): Int

    fun insert(accounts: List<Account>)

    fun insert(account: Account): Boolean

    fun filter(conditions: List<FieldCondition>, limit: Int): List<Account>

    fun group(query: GroupQuery, limit: Int, order: Int): List<AccountGroup>
}

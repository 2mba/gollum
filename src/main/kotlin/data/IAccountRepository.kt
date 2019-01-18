package org.tumba.gollum.domain.repository

import domain.FieldCondition
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountPatch

interface IAccountRepository {
    fun insert(accounts: List<Account>)

    fun insert(account: Account): Boolean

    fun update(id: Long, accountPatch: AccountPatch): Boolean

    fun filter(conditions: List<FieldCondition>, limit: Int): List<Account>
}

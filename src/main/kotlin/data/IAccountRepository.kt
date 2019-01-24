package org.tumba.gollum.domain.repository

import domain.FieldCondition
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountPatch
import org.tumba.gollum.domain.entities.Group

interface IAccountRepository {
    fun insert(accounts: List<Account>)

    fun insert(account: Account): Boolean

    fun update(id: Int, accountPatch: AccountPatch): Boolean

    fun filter(conditions: List<FieldCondition>, limit: Int): List<Account>

    fun group(keys: List<String>, conditions: List<FieldCondition>, limit: Int, order: Int): List<Group>
}

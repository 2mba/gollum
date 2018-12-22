package org.tumba.gollum.domain.repository

import domain.repository.FieldCondition
import org.tumba.gollum.domain.entities.Account

interface IAccountRepository {
    fun size(): Int

    fun insert(accounts: List<Account>)

    fun filter(conditions: List<FieldCondition>, limit: Int): List<Account>
}



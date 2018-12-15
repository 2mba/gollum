package org.tumba.gollum.data

import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.repository.IAccountRepository

class AccountRepository: IAccountRepository {

    override fun insert(vararg accounts: Account) {
        TODO("not implemented")
    }

    override fun filter(query: IAccountRepository.FilterQuery): List<Account> {
        TODO("not implemented")
    }
}
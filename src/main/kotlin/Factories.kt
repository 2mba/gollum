package org.tumba.gollum

import org.jetbrains.exposed.sql.Database
import org.tumba.gollum.data.mongo.SqlAccountRepository
import org.tumba.gollum.domain.repository.IAccountRepository

object Factories {

    val accountRepositoryFactory: AccountRepositoryFactory = AccountRepositoryFactory()
}

class AccountRepositoryFactory {

    fun getAccountRepository(): IAccountRepository = repository

    companion object {

        private fun createSqlAccountRepository(): IAccountRepository {
            val database = Database.connect(
                url = "jdbc:mysql://localhost:3306/accounts",
                driver = "com.mysql.jdbc.Driver",
                user = "highload",
                password = "highload"
            )
            return SqlAccountRepository(database)
        }

        private val repository: IAccountRepository by lazy { createSqlAccountRepository() }
    }
}


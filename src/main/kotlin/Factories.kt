package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import org.jetbrains.exposed.sql.Database
import org.tumba.gollum.data.mongo.SqlAccountRepository
import org.tumba.gollum.domain.repository.IAccountRepository
import java.sql.DriverManager
import java.sql.SQLException


object Factories {

    val accountRepositoryFactory: AccountRepositoryFactory = AccountRepositoryFactory()

    val dslJsonFactory: DslJsonFactory = DslJsonFactory()
}

class DslJsonFactory {

    fun getDslJson() = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader())
}

class AccountRepositoryFactory {

    fun getAccountRepository(): IAccountRepository = repository

    companion object {

        private fun createSqlAccountRepository(): IAccountRepository {
            createDatabase()
            val database = Database.connect(
                url = "jdbc:sqlite:sqlite/db/accounts",
                driver = "org.sqlite.JDBC",
                user = "highload",
                password = "highload"
            )
            return SqlAccountRepository(database)
        }

        private fun createDatabase() {
            val url = "jdbc:sqlite:sqlite/db/accounts"

            try {
                DriverManager.getConnection(url).use { conn ->
                    if (conn != null) {
                        val meta = conn.metaData
                        println("The driver name is " + meta.driverName)
                        println("A new database has been created.")
                    }

                }
            } catch (e: SQLException) {
                println(e.message)
            }
        }

        private val repository: IAccountRepository by lazy { createSqlAccountRepository() }
    }
}


object Main {

    /**
     * Connect to a sample database
     *
     * @param fileName the database file name
     */
    fun createNewDatabase(fileName: String) {

        val url = "jdbc:sqlite:C:/sqlite/db/$fileName"

        try {
            DriverManager.getConnection(url).use { conn ->
                if (conn != null) {
                    val meta = conn.metaData
                    println("The driver name is " + meta.driverName)
                    println("A new database has been created.")
                }

            }
        } catch (e: SQLException) {
            println(e.message)
        }

    }

    /**
     * @param args the command line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        createNewDatabase("test.db")
    }
}
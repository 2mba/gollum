package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database
import org.tumba.gollum.data.mongo.SqlAccountRepository
import org.tumba.gollum.domain.repository.IAccountRepository
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {
    val port = 80
    val dataPath = "/tmp/data/data.zip"
    val optionsPath = "/tmp/data/options.txt"
    val optionsLines = File(optionsPath).readLines()
    val optionsNow = optionsLines[0].trim().toLong()

    val accountRepository = createAccountRepository(optionsNow)
    val dslJson = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader().skipDefaultValues(true))

    val dataImporter = AccountsImporter(accountRepository, dslJson, dataPath)

    //measureTimeMillis { dataImporter.import() }.also { println("Import $it ms") }

    val routes = Routes(accountRepository, dslJson)

    embeddedServer(Netty, port) {
        routing { routes.getRoute(this) }
    }.start(wait = true)
}

const val databasePath = "/Users/obairka/Projects/gollum/"
const val databaseConnectionString = "jdbc:sqlite:${databasePath}sqlite/db/accounts"

fun createAccountRepository(now: Long): IAccountRepository {
    createDatabase()
    val database = Database.connect(
        url = databaseConnectionString,
        driver = "org.sqlite.JDBC",
        user = "highload",
        password = "highload"
    )

    return SqlAccountRepository(database, now)
}

fun createDatabase() {
    try {
        DriverManager.getConnection(databaseConnectionString).use { conn ->
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
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
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {
    val port = 80
    val dataPath = "/tmp/data/data.zip"
    val optionsPath = "/tmp/data/options.txt"
    val optionsLines = File(optionsPath).readLines()
    val optionsNow = optionsLines[0].trim().toLong()

    val accountRepository = createAccountRepository(optionsNow)
    val dslJson = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader().skipDefaultValues(true))

    Timer("memoryusage", false).schedule(5000, 90000) {
        val bytesUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("Memory Usage: ${bytesUsage/1024/1024} mb (${bytesUsage} bytes)")
    }
    val dataImporter = AccountsImporter(accountRepository, dslJson, dataPath)

    measureTimeMillis { dataImporter.import() }.also { println("Import $it ms") }

    val routes = Routes(accountRepository, dslJson)

    embeddedServer(Netty, port = port) {
        routing { routes.getRoute(this) }
    }.start(wait = true)
}

fun createAccountRepository(now: Long): IAccountRepository {
    val database = Database.connect(
        "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1",
        "org.h2.Driver"
    )
    return SqlAccountRepository(database, now)
}
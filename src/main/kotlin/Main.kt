package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.tumba.gollum.data.sql.MemoryRepository
import org.tumba.gollum.domain.repository.IAccountRepository
import java.io.File
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

    measureTimeMillis { dataImporter.import() }.also { println("Import $it ms") }

    val routes = Routes(accountRepository, dslJson)

    embeddedServer(Netty, port = port) {
        routing { routes.getRoute(this) }
    }.start(wait = true)
}

fun createAccountRepository(now: Long): IAccountRepository {
    return MemoryRepository(now)
}
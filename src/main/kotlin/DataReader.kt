package org.tumba.gollum

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.Account

@CompiledJson
data class AccountList(
    val accounts: ArrayList<Account>
)

class DataReader {
    fun test() {
        val source = "/tmp/data.zip"
        val json = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader())

        try {
            val zipFile = ZipFile(source)
            val fileHeaders = zipFile.fileHeaders as List<FileHeader>
            fileHeaders
                .filter { it -> it.fileName.startsWith("accounts") }
                .forEach { header ->
                    zipFile.getInputStream(header).use { stream ->
                        val accounts = json.deserialize(AccountList::class.java, stream)
                        println(accounts.accounts.size)
                        accounts.accounts.asSequence()
                            .take(5)
                            .forEach { println(it) }
                    }
                }
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}
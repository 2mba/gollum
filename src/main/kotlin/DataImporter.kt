package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.AccountList


class DataImporter(private val accountRepository: AccountRepository, private val path: String) {
    fun import() {
        val dslJson = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader())

        try {
            val zipFile = ZipFile(path)
            val fileHeaders = zipFile.fileHeaders as List<FileHeader>
            fileHeaders
                .filter { it -> it.fileName.startsWith("accounts_") }
                .forEach { header ->
                    zipFile.getInputStream(header).use { stream ->
                        val accountList = dslJson.deserialize(AccountList::class.java, stream)
                        println("${header.fileName}: Accounts ${accountList.accounts.size}")
                        val chunked = accountList.accounts.chunked(30000)
                        println("Memory usage. Total: ${Runtime.getRuntime().totalMemory()} Free: ${Runtime.getRuntime().freeMemory()}")

                        chunked.forEach {
                            for (account in it) {
                                accountRepository.tryInsert(account)
                            }
                        }
                    }
                }
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}
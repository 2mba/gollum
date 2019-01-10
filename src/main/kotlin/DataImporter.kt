package org.tumba.gollum

import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.AccountList
import org.tumba.gollum.domain.repository.IAccountRepository


class DataImporter(private val accountRepository: IAccountRepository, private val path: String) {
    fun import() {
        val dslJson = Factories.dslJsonFactory.getDslJson()

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
                            accountRepository.insert(it)
                        }
                    }
                }
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}
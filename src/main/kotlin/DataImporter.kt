package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.AccountList
import org.tumba.gollum.domain.repository.IAccountRepository


class DataImporter(private val accountRepository: IAccountRepository) {
    fun import() {
        val source = "/tmp/data.zip"
        val dslJson = DslJson<Any>(Settings.withRuntime<Any>().includeServiceLoader())

        try {
            val zipFile = ZipFile(source)
            val fileHeaders = zipFile.fileHeaders as List<FileHeader>
            fileHeaders
                .filter { it -> it.fileName.startsWith("accounts_") }
                .forEach { header ->
                    zipFile.getInputStream(header).use { stream ->
                        val accountList = dslJson.deserialize(AccountList::class.java, stream)
                        accountRepository.insert(accountList.accounts)
                    }
                }
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}
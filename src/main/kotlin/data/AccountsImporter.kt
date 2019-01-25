package org.tumba.gollum

import com.dslplatform.json.DslJson
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.AccountList
import org.tumba.gollum.domain.repository.IAccountRepository


class AccountsImporter(private val accountRepository: IAccountRepository,
                       private val dslJson: DslJson<Any>, private val path: String) {
    fun import() {
        try {
            val zipFile = ZipFile(path)
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
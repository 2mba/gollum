package org.tumba.gollum

import com.dslplatform.json.DslJson
import com.dslplatform.json.runtime.Settings
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountList

// временный репозиторий
interface AccountRepository {
    fun save(accounts: List<Account>)
    fun size(): Int
}

class InMemoryAccountRepository : AccountRepository {
    private val accounts: ArrayList<Account> = ArrayList()

    override fun save(accounts: List<Account>) {
        this.accounts.addAll(accounts)
    }

    override fun size(): Int {
        return accounts.size
    }
}

class DataImporter(private val accountRepository: AccountRepository) {
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
                        accountRepository.save(accountList.accounts)
                    }
                }
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}
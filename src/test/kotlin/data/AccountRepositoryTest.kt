package data

import org.amshove.kluent.`should be in`
import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test
import org.tumba.gollum.data.AccountRepository
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.Status
import org.tumba.gollum.domain.repository.FieldCondition
import org.tumba.gollum.domain.repository.IAccountRepository

internal class AccountRepositoryTest {

    @Test
    fun insert() {
        TODO()
    }

    @Test
    fun filter() {
        val repository =  AccountRepository()
        val accounts = repository.filter(
            query = IAccountRepository.FilterQuery(
                status = FieldCondition.Equal(Status.FREE)
            )
        )
        accounts shouldBe arrayOf<Account>()
    }
}
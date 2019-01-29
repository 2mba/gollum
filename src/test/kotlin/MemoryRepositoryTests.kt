import domain.FieldCondition
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test
import org.tumba.gollum.data.sql.MemoryRepository
import org.tumba.gollum.domain.entities.*

class MemoryRepositoryTests {

    @Test
    fun testInterests() {
        val repository = MemoryRepository(0)

        repository.insert(
            accounts = listOf(
                account(
                    id = 0,
                    email = "aa0@aa.com",
                    interests = (0..100).asSequence().map { it.toString() }.toCollection(arrayListOf())
                ),
                account(
                    id = 1,
                    email = "aa1@aa.com",
                    interests = arrayListOf(
                        "1", "2"
                    )
                ),
                account(
                    id = 2,
                    email = "aa2@aa.com",
                    interests = arrayListOf(
                        "4", "2", "5", "100"
                    )
                ),
                account(
                    id = 3,
                    email = "aa3@aa.com",
                    interests = arrayListOf(
                        "1", "3", "5"
                    )
                )
            )
        )

        val result = repository.filter(
            conditions = listOf(
                FieldCondition("interests", "contains", "100")
            ),
            limit = 99
        )
        result[0].id `should be`  2
        result[1].id `should be`  0
        result.size `should be`  2

        val result2 = repository.filter(
            conditions = listOf(
                FieldCondition("interests", "any", "1")
            ),
            limit = 99
        )
        result2[0].id `should be`  3
        result2[1].id `should be`  1
        result2[2].id `should be`  0
        result2.size `should be`  3

        val result3 = repository.filter(
            conditions = listOf(
                FieldCondition("interests", "any", "99")
            ),
            limit = 99
        )
        result3[0].id `should be`  0
        result3.size `should be`  1
    }
}

private fun account(
    id: Int,
    email: String,
    fname: String? = null,
    sname: String? = null,
    phone: String? = null,
    sex: Sex? = Sex.MALE,
    birth: Long? = null,
    country: String? = "aa",
    city: String? = "bb",
    joined: Long? = 1,
    status: Status? = Status.FREE,
    interests: ArrayList<String>? = null,
    premium: Premium? = null,
    likes: ArrayList<Like>? = null
): Account {
    return Account(
        id = id,
        email = email,
        fname = fname,
        sname = sname,
        phone = phone,
        sex = sex,
        birth = birth,
        country = country,
        city = city,
        joined = joined,
        status = status,
        interests = interests,
        premium = premium,
        likes = likes
    )
}

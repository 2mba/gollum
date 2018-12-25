package org.tumba.gollum.data.mongo

import com.mongodb.MongoClient
import domain.repository.FieldCondition
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo
import org.tumba.gollum.DataImporter


class MongoAccountRepositoryTest {
    private lateinit var mongoClient: MongoClient
    private lateinit var repository: MongoAccountRepository

    @BeforeEach
    fun before() {
        mongoClient = KMongo.createClient("localhost")
        repository = MongoAccountRepository(mongoClient, "unitTest")

//        val dataImporter = DataImporter(repository)
//        dataImporter.import()
    }

    @Test
    fun filterBySexTest() {
        val males = repository.filter(
            arrayListOf(FieldCondition("sex", "eq", "m")),
            limit = 1
        )
        males.size shouldBe 1
        males[0].sex `should be equal to` "m"

        val females = repository.filter(
            arrayListOf(FieldCondition("sex", "eq", "f")),
            limit = 1
        )
        females.size shouldBe 1
        females[0].sex `should be equal to` "f"
    }

    @Test
    fun filterByEmailDomainTest() {
        val accounts = repository.filter(
            arrayListOf(FieldCondition("email", "domain", "inbox.com")),
            limit = 1
        )
        accounts.size shouldBe 1
        accounts[0].email `should be equal to` "hoodryruf@inbox.com"
    }

    @Test
    fun filterByEmailLtGtTest() {
        val accounts = repository.filter(
            arrayListOf(FieldCondition("email", "lt", "hz")),
            limit = 1
        )
        accounts.size shouldBe 1
        accounts[0].email `should be equal to` "hoodryruf@inbox.com"

        val accounts2 = repository.filter(
            arrayListOf(FieldCondition("email", "gt", "ha")),
            limit = 1
        )
        accounts2.size shouldBe 1
        accounts2[0].email `should be equal to` "tohahir@mail.ru"
    }

    @Test
    fun filterByStatusTest() {
        val accounts = repository.filter(
            arrayListOf(FieldCondition("status", "eq", "всё сложно")),
            limit = 1
        )
        accounts.size shouldBe 1
        accounts[0].id shouldBe 3

        val accounts2 = repository.filter(
            arrayListOf(FieldCondition("status", "neq", "заняты")),
            limit = 1
        )
        accounts2.size shouldBe 1
        accounts2[0].id shouldBe 2
    }

    @Test
    fun filterByFnameTest() {
        val eq = repository.filter(
            arrayListOf(FieldCondition("fname", "eq", "Фёдор")),
            limit = 1
        )
        eq.size shouldBe 1
        eq[0].id shouldBe 3

        val any = repository.filter(
            arrayListOf(FieldCondition("fname", "any", "Катя,Фёдор")),
            limit = 1
        )
        any.size shouldBe 1
        any[0].id shouldBe 3

        val null0 = repository.filter(
            arrayListOf(FieldCondition("fname", "null", "0")),
            limit = 1
        )
        null0.size shouldBe 1
        null0[0].id shouldBe 1


        val null1 = repository.filter(
            arrayListOf(FieldCondition("fname", "null", "1")),
            limit = 1
        )
        null1.size shouldBe 1
        null1[0].id shouldBe 9
    }

    @Test
    fun filterByStartsTest() {
        val eq = repository.filter(
            arrayListOf(FieldCondition("sname", "eq", "Лукушуко")),
            limit = 1
        )
        eq.size shouldBe 1
        eq[0].id shouldBe 2

        val starts = repository.filter(
            arrayListOf(FieldCondition("sname", "starts", "Луку")),
            limit = 1
        )
        starts.size shouldBe 1
        starts[0].id shouldBe 2

        val null0 = repository.filter(
            arrayListOf(FieldCondition("sname", "null", "0")),
            limit = 1
        )
        null0.size shouldBe 1
        null0[0].id shouldBe 2


        val null1 = repository.filter(
            arrayListOf(FieldCondition("sname", "null", "1")),
            limit = 1
        )
        null1.size shouldBe 1
        null1[0].id shouldBe 1
    }
}
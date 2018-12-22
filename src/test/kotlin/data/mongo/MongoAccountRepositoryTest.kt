package org.tumba.gollum.data.mongo

import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo


class MongoAccountRepositoryTest {
    private lateinit var repository: MongoAccountRepository;

    @Before
    fun before() {
        val mongoClient = KMongo.createClient("localhost")
    }

    @Test
    fun test() {
        2 * 2 shouldBe 4
    }
}
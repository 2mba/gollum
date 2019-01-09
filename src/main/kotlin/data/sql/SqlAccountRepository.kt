package org.tumba.gollum.data.mongo

import domain.repository.FieldCondition
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountPatch
import org.tumba.gollum.domain.repository.AccountGroup
import org.tumba.gollum.domain.repository.GroupQuery
import org.tumba.gollum.domain.repository.IAccountRepository

class SqlAccountRepository(val database: Database) : IAccountRepository {

    init {
        init()
    }

    fun init() {
        transaction(database) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(AccountsTable)
        }
    }

    override fun size(): Int {
        TODO("not implemented")
    }

    override fun insert(accounts: List<Account>) {
        transaction(database) {
            AccountsTable.batchInsert(accounts.toList(), ignore = false) { account ->
                AccountsTable.toDbEntity(this, account)
            }
        }
    }

    override fun insert(account: Account): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(id: Long, accountPatch: AccountPatch): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        return transaction(database) {
            AccountsTable.select {
                whereStatementBuilder {
                    conditionsOf(
                        /*condition(AccountsTable.firstName, query.firstName),
                        condition(AccountsTable.surName, query.surName),
                        condition(AccountsTable.sex, query.sex?.convert { SexDbMapper.toDbValue(it) }),
                        condition(AccountsTable.status, query.status?.convert { StatusDbMapper.toDbValue(it) })*/
                    )
                } ?: throw IllegalStateException("!!!")
            }.asSequence().map { row -> row.toAccount() }.toList()
        }
    }

    override fun group(query: GroupQuery, limit: Int, order: Int): List<AccountGroup> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

fun whereStatementBuilder(block: WhereStatementBuilder.() -> Op<Boolean>?) = block.invoke(WhereStatementBuilder)

object WhereStatementBuilder {

    fun conditionsOf(vararg conditions: Op<Boolean>?): Op<Boolean>? {
        var op: Op<Boolean>? = null
        conditions.forEach { condition ->
            condition ?: return@forEach
            op = if (op == null) {
                condition
            } else {
                op!! and condition
            }
        }
        return op
    }

    fun <T> SqlExpressionBuilder.condition(column: Column<T?>, condition: FieldCondition?): Op<Boolean>? {
        condition ?: return null
        return TODO() /* when (condition) {
            is FieldCondition.Equal -> column eq condition.value
            is FieldCondition.NotEqual -> column neq condition.value
            is FieldCondition.LessThen -> TODO()
            is FieldCondition.GreaterThen -> TODO()
            is FieldCondition.Any -> column inList condition.values
            is FieldCondition.Contains -> TODO()
        } */
    }
}

class SqlAccountRepositoryFactory {

    fun create(): SqlAccountRepository {
        val database = Database.connect(
            url = "jdbc:mysql://localhost:3306/accounts",
            driver = "com.mysql.jdbc.Driver",
            user = "highload",
            password = "highload"
        )
        return SqlAccountRepository(database)
    }
}

private object AccountsTable : IntIdTable() {
    val firstName = varchar("firstName", 50).nullable()
    val surName = varchar("surName", 50).nullable()
    val email = varchar("email", 100)
    val interests = varchar("interests", 50).nullable() // todo
    val status = integer("status").nullable()
    val premium = long("premium").nullable() // todo
    val sex = integer("sex").nullable()
    val phone = varchar("phone", 50).nullable()
    val likes = varchar("likes", 50).nullable() // todo
    val birth = date("birth").nullable()
    val city = varchar("city", 50).nullable()
    val country = varchar("country", 50).nullable()
    val joined = date("joined").nullable()
}

private fun AccountsTable.toDbEntity(insertStatement: InsertStatement<*>, account: Account) {
    insertStatement[id] = EntityID(account.id.toInt(), AccountsTable)
    insertStatement[firstName] = account.fname
    insertStatement[surName] = account.sname
    insertStatement[email] = account.email
    //insertStatement[interests] = account.interests
    //insertStatement[status] = StatusDbMapper.toDbValue(account.status)
    //insertStatement[premium] = account.premium
    //insertStatement[sex] = SexDbMapper.toDbValue(account.sex)
    insertStatement[phone] = account.phone
    //insertStatement[likes] = account.likes
    insertStatement[birth] = DateTime(account.birth)
    insertStatement[city] = account.city
    insertStatement[country] = account.country
    insertStatement[joined] = DateTime(account.joined)
}

private fun ResultRow.toAccount(): Account {
    return Account(
        id = this[AccountsTable.id].value.toLong(),
        fname = this[AccountsTable.firstName],
        sname = this[AccountsTable.surName],
        email = this[AccountsTable.email],
        interests = null, //this[AccountsTable.interests],
        status = "",//StatusDbMapper.fromDbValue(this[AccountsTable.status]) ?: throwError("status"),
        premium = null, // this[AccountsTable.premium],
        sex = "", // SexDbMapper.fromDbValue(this[AccountsTable.sex]) ?: throwError("sex"),
        phone = this[AccountsTable.phone],
        likes = null, //this[AccountsTable.likes],
        birth = this[AccountsTable.birth]?.millis ?: throwError("birth"),
        city = this[AccountsTable.city],
        country = this[AccountsTable.country],
        joined = this[AccountsTable.joined]?.millis ?: throwError("joined")
    )
}

// private object SexDbMapper : EnumDbMapper<Sex>(Sex.values())

// private object StatusDbMapper : EnumDbMapper<Status>(Status.values())

private open class DbMapper<T : Enum<T>>(private val values: Array<T>) {

    fun toDbValue(t: T): Int = t.ordinal

    fun fromDbValue(value: Int?, default: T? = null): T? = if (value != null) values[value] else default
}

private fun throwError(fieldName: String): Nothing = throw NullPointerException("Field $fieldName can not be null")
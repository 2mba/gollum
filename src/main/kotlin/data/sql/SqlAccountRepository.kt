package org.tumba.gollum.data.mongo

import domain.FieldCondition
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.tumba.gollum.domain.entities.Account
import org.tumba.gollum.domain.entities.AccountPatch
import org.tumba.gollum.domain.entities.Sex
import org.tumba.gollum.domain.entities.Status
import org.tumba.gollum.domain.repository.IAccountRepository
import java.sql.Connection
import java.time.LocalDate

class SqlAccountRepository(val database: Database, val timestamp: Long) : IAccountRepository {

    init {
        init()
    }

    fun init() {
        transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(AccountsTable)
        }
    }

    override fun insert(accounts: List<Account>) {
        transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {
            AccountsTable.batchInsert(accounts.toList(), ignore = false) { account ->
                AccountsTable.toDbEntity(this, account)
            }
        }
    }

    override fun insert(account: Account): Boolean {
        try {
            transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {
                AccountsTable.insert { AccountsTable.toDbEntity(it, account) }
            }
            return true
        }
        catch (ex: Throwable) {
            return false
        }
    }

    override fun update(id: Long, accountPatch: AccountPatch): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        val stringColumns = hashMapOf(
            Pair("fname", AccountsTable.fname),
            Pair("sname", AccountsTable.sname),
            Pair("phone", AccountsTable.phone),
            Pair("country", AccountsTable.country),
            Pair("city", AccountsTable.city)
        )
        val usedColumns = ArrayList<Column<*>>()
        usedColumns.add(AccountsTable.id)
        usedColumns.add(AccountsTable.email)
        val filters = conditions.map { condition ->
            when (condition.fieldName) {
                "fname", "sname", "phone", "country", "city" -> {
                    val column = stringColumns[condition.fieldName]!!
                    usedColumns.add(column)
                    when (condition.predicate) {
                        "eq" -> column eq condition.value
                        "neq" -> column neq condition.value
                        "any" -> column inList condition.value.split(',')
                        "null" ->
                            if (condition.value == "0")
                                column.isNotNull()
                            else
                                column.isNull()
                        "starts" -> LikeOp(column, QueryParameter("${condition.value}%", column.columnType))
                        "code" -> LikeOp(column, QueryParameter("%(${condition.value})%", column.columnType))
                        else -> illegalCondition(condition)
                    }
                }
                "status" -> {
                    usedColumns.add(AccountsTable.status)
                    val status = StatusDbMapper.toDbValue(when (condition.value) {
                        Status.BUSY.value -> Status.BUSY
                        Status.COMPLICATED.value -> Status.COMPLICATED
                        Status.FREE.value -> Status.FREE
                        else -> illegalCondition(condition)
                    })
                    when (condition.predicate) {
                        "eq" -> AccountsTable.status eq status
                        "neq" -> AccountsTable.sex neq status
                        else -> illegalCondition(condition)
                    }
                }
                "email" -> {
                    val column = AccountsTable.email
                    when (condition.predicate) {
                        "domain" -> LikeOp(column, QueryParameter("%@${condition.value}", column.columnType))
                        "lt" -> column less condition.value
                        "gt" -> column greater condition.value
                        else -> illegalCondition(condition)
                    }
                }
                "sex" -> {
                    usedColumns.add(AccountsTable.sex)
                    val sex = SexDbMapper.toDbValue(when (condition.value) {
                        Sex.MALE.value -> Sex.MALE
                        Sex.FEMALE.value -> Sex.FEMALE
                        else -> illegalCondition(condition)
                    })
                    when (condition.predicate) {
                        "eq" -> AccountsTable.sex eq sex
                        "neq" -> AccountsTable.sex neq sex
                        else -> illegalCondition(condition)
                    }
                }
                "birth" -> {
                    usedColumns.add(AccountsTable.birth)
                    when (condition.predicate) {
                        "lt" -> AccountsTable.birth less condition.value.toLong()
                        "gt" -> AccountsTable.birth greater condition.value.toLong()
                        "year" -> {
                            val year = condition.value.toInt()
                            val yearStart = LocalDate.of(year, 1, 1).toEpochDay() * 60 * 60 * 24
                            val yearEnd = LocalDate.of(year+1, 1, 1).toEpochDay()* 60 * 60 * 24

                            (AccountsTable.birth greater yearStart) and (AccountsTable.birth less yearEnd)
                        }
                        else -> illegalCondition(condition)
                    }

                }
                else -> illegalCondition(condition)
            }
        }

        return transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {

            val query = if (filters.isNotEmpty()) {
                AccountsTable.select {
                        combineFilters(filters)
                    }
                    .adjustSlice {
                        slice(usedColumns)
                    }
            } else {
                AccountsTable.selectAll()
                    .adjustSlice {
                        slice(AccountsTable.id, AccountsTable.email, AccountsTable.fname)
                    }
            }

            query
                .orderBy(AccountsTable.id, isAsc = false)
                .limit(limit)
                .asSequence()
                .map { r -> r.toAccount() }
                .toList()
        }
    }
}

private object AccountsTable : IntIdTable() {
    val fname = varchar("fname", 50).nullable()
    val sname = varchar("sname", 50).nullable()
    val email = varchar("email", 100)
    //val interests = varchar("interests", 50).nullable()
    val status = integer("status").nullable()
    //val premium = long("premium").nullable() // todo
    val sex = integer("sex").nullable()
    val phone = varchar("phone", 50).nullable()
    //val likes = varchar("likes", 50).nullable()
    val birth = long("birth").nullable()
    val city = varchar("city", 50).nullable()
    val country = varchar("country", 50).nullable()
    val joined = long("joined").nullable()
}

private fun AccountsTable.toDbEntity(insertStatement: InsertStatement<*>, account: Account) {
    insertStatement[id] = EntityID(account.id.toInt(), AccountsTable)
    insertStatement[fname] = account.fname
    insertStatement[sname] = account.sname
    insertStatement[email] = account.email
    //insertStatement[interests] = account.interests
    insertStatement[status] = StatusDbMapper.toDbValue(account.status!!)
    //insertStatement[premium] = account.premium
    insertStatement[sex] = SexDbMapper.toDbValue(account.sex!!)
    insertStatement[phone] = account.phone
    //insertStatement[likes] = account.likes
    insertStatement[birth] = account.birth
    insertStatement[city] = account.city
    insertStatement[country] = account.country
    insertStatement[joined] = account.joined
}

private fun ResultRow.toAccount(): Account {
    return Account(
        id = this[AccountsTable.id].value.toLong(),
        fname = if (this.hasValue(AccountsTable.fname)) this[AccountsTable.fname] else null,
        sname = if (this.hasValue(AccountsTable.sname)) this[AccountsTable.sname] else null,
        email = this[AccountsTable.email],
        interests = null, //this[AccountsTable.interests],
        status = if (this.hasValue(AccountsTable.status)) StatusDbMapper.fromDbValue(this[AccountsTable.status]) else null,
        premium = null, // this[AccountsTable.premium],
        sex = if (this.hasValue(AccountsTable.sex)) SexDbMapper.fromDbValue(this[AccountsTable.sex]) else null,
        phone = if (this.hasValue(AccountsTable.phone)) this[AccountsTable.phone] else null,
        likes = null, //this[AccountsTable.likes],
        birth = if (this.hasValue(AccountsTable.birth)) this[AccountsTable.birth] else null,
        city = if (this.hasValue(AccountsTable.city)) this[AccountsTable.city] else null,
        country = if (this.hasValue(AccountsTable.country)) this[AccountsTable.country] else null,
        joined = if (this.hasValue(AccountsTable.joined)) this[AccountsTable.joined] else null
    )
}

private object SexDbMapper : EnumDbMapper<Sex>(Sex.values())

private object StatusDbMapper : EnumDbMapper<Status>(Status.values())

private open class EnumDbMapper<T : Enum<T>>(private val values: Array<T>) {

    fun toDbValue(t: T): Int = t.ordinal

    fun fromDbValue(value: Int?, default: T? = null): T? = if (value != null) values[value] else default
}

fun combineFilters(conditions: List<Op<Boolean>>): Op<Boolean> {
    var op: Op<Boolean> = conditions.first()
    conditions
        .drop(1)
        .forEach { op = op and it }
    return op
}

private fun illegalCondition(condition: FieldCondition): Nothing = throw IllegalArgumentException("Illegal condition ${condition.fieldName} ${condition.predicate} ${condition.value}")
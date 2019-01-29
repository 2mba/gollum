package org.tumba.gollum.data.mongo

import domain.FieldCondition
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.tumba.gollum.domain.entities.*
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

            AccountsTable.batchInsert(accounts, ignore = false) { account ->
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

    override fun update(id: Int, accountPatch: AccountPatch): Boolean {
        try {
            transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {
                AccountsTable.update {
                    AccountsTable.toDbEntity(it, accountPatch, id)
                }
            }
            return true
        }
        catch (ex: Throwable) {
            return false
        }
    }

    override fun filter(conditions: List<FieldCondition>, limit: Int): List<Account> {
        val (usedColumns, filters) = createFilters(conditions)

        return transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {

            val query = if (filters.isNotEmpty()) {
                AccountsTable
                    .slice(usedColumns)
                    .select {
                        combineFilters(filters)
                    }
            } else {
                AccountsTable
                    .slice(AccountsTable.id, AccountsTable.email)
                    .selectAll()
            }

            query
                .orderBy(AccountsTable.id, isAsc = false)
                .limit(limit)
                .asSequence()
                .map { r -> r.toAccount() }
                .toList()
        }
    }

    override fun group(keys: List<String>, conditions: List<FieldCondition>, limit: Int, order: Int): List<Group> {
        val (_, filters) = createFilters(conditions)
        val groupBy = keys.map {
            when (it) {
                "status" -> AccountsTable.status
                "sex" -> AccountsTable.sex
                "country" -> AccountsTable.country
                "city" -> AccountsTable.city
                else -> illegalArgument()
            }
        }.toTypedArray()

        return transaction(Connection.TRANSACTION_SERIALIZABLE, 1, database) {

            val query = if (filters.isNotEmpty()) {
                AccountsTable
                    .slice(*groupBy, AccountsTable.id.count())
                    .select {
                        combineFilters(filters)
                    }
            } else {
                AccountsTable
                    .slice(*groupBy, AccountsTable.id.count())
                    .selectAll()
            }
            val isAsc = order == 1
            val orderBy = groupBy
                .map { Pair(it, isAsc) }
                .toTypedArray()

            query
                .groupBy(*groupBy)
                .orderBy(Pair(AccountsTable.id.count(), isAsc), *orderBy)
                .limit(limit)
                .asSequence()
                .map { r -> r.toGroup() }
                .toList()
        }
    }

    private fun createFilters(conditions: List<FieldCondition>): Pair<List<Column<*>>, List<Op<Boolean>>> {
        val usedColumns = ArrayList<Column<*>>()
        usedColumns.add(AccountsTable.id)
        usedColumns.add(AccountsTable.email)
        val filters = conditions
            .asSequence()
            .filter { c -> c.fieldName != "interests" && c.fieldName != "likes" }
            .map { condition ->
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
                        val status = StatusDbMapper.toDbValue(
                            when (condition.value) {
                                Status.BUSY.value -> Status.BUSY
                                Status.COMPLICATED.value -> Status.COMPLICATED
                                Status.FREE.value -> Status.FREE
                                else -> illegalCondition(condition)
                            }
                        )
                        when (condition.predicate) {
                            "eq" -> AccountsTable.status eq status
                            "neq" -> AccountsTable.status neq status
                            else -> illegalCondition(condition)
                        }
                    }
                    "email" -> {
                        val column = AccountsTable.email
                        when (condition.predicate) {
                            "domain" -> AccountsTable.email_domain eq condition.value
                            "lt" -> column less condition.value
                            "gt" -> column greater condition.value
                            else -> illegalCondition(condition)
                        }
                    }
                    "sex" -> {
                        usedColumns.add(AccountsTable.sex)
                        val sex = SexDbMapper.toDbValue(
                            when (condition.value) {
                                Sex.MALE.value -> Sex.MALE
                                Sex.FEMALE.value -> Sex.FEMALE
                                else -> illegalCondition(condition)
                            }
                        )
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
                                val yearEnd = LocalDate.of(year + 1, 1, 1).toEpochDay() * 60 * 60 * 24

                                (AccountsTable.birth greater yearStart) and (AccountsTable.birth less yearEnd)
                            }
                            else -> illegalCondition(condition)
                        }
                    }
                    "premium" -> {
                        usedColumns.add(AccountsTable.premium_start)
                        usedColumns.add(AccountsTable.premium_finish)
                        when (condition.predicate) {
                            "null" ->
                                if (condition.value == "0")
                                    AccountsTable.premium_start.isNotNull()
                                else
                                    AccountsTable.premium_start.isNull()
                            "now" -> {
                                (AccountsTable.premium_start less timestamp.inc()) and (AccountsTable.premium_finish greater timestamp.dec())
                            }
                            else -> illegalCondition(condition)
                        }
                    }
                    else -> illegalCondition(condition)
                }
            }
            .toList()
        return Pair(usedColumns, filters)
    }
}

private object AccountsTable : Table() {
    val id: Column<Int> = integer("id").primaryKey()
    val fname = varchar("fname", 50).nullable()
    val sname = varchar("sname", 50).nullable()
    val email = varchar("email", 100).index(isUnique = true)
    val email_domain = varchar("email_domain", 100)
    val status = integer("status").nullable() //.index()
    val premium_start = long("premium_start").nullable()
    val premium_finish = long("premium_finish").nullable()
    val sex = integer("sex").nullable() //.index()
    val phone = varchar("phone", 50).nullable()
    val birth = long("birth").nullable()
    val city = varchar("city", 50).nullable() //.index()
    val country = varchar("country", 50).nullable() //.index()
    val joined = long("joined").nullable()
}


private val stringColumns = hashMapOf(
    Pair("fname", AccountsTable.fname),
    Pair("sname", AccountsTable.sname),
    Pair("phone", AccountsTable.phone),
    Pair("country", AccountsTable.country),
    Pair("city", AccountsTable.city)
)

private fun AccountsTable.toDbEntity(insertStatement: InsertStatement<*>, account: Account) {
    insertStatement[id] = account.id
    insertStatement[fname] = account.fname
    insertStatement[sname] = account.sname
    insertStatement[email] = account.email
    insertStatement[email_domain] = account.email.split('@')[1]
    insertStatement[status] = StatusDbMapper.toDbValue(account.status!!)
    insertStatement[premium_start] = if (account.premium == null) null else account.premium.start
    insertStatement[premium_finish] = if (account.premium == null) null else account.premium.finish
    insertStatement[sex] = SexDbMapper.toDbValue(account.sex!!)
    insertStatement[phone] = account.phone
    insertStatement[birth] = account.birth
    insertStatement[city] = account.city
    insertStatement[country] = account.country
    insertStatement[joined] = account.joined
}

private fun AccountsTable.toDbEntity(statement: UpdateStatement, account: AccountPatch, idVal: Int) {
    statement[id] = idVal
    statement[fname] = account.fname
    statement[sname] = account.sname
    if (account.email != null) {
        statement[email] = account.email
        statement[email_domain] = account.email.split('@')[1]
    }
    if (account.status != null) {
        statement[status] = StatusDbMapper.toDbValue(account.status)
    }
    if (account.premium != null) {
        statement[premium_start] = account.premium.start
        statement[premium_finish] = account.premium.finish
    }
    if (account.sex != null) {
        statement[sex] = SexDbMapper.toDbValue(account.sex)
    }
    if (account.phone != null) {
        statement[phone] = account.phone
    }
    if (account.birth != null) {
        statement[birth] = account.birth
    }
    if (account.city != null) {
        statement[city] = account.city
    }
    if (account.country != null) {
        statement[country] = account.country
    }
    if (account.joined != null) {
        statement[joined] = account.joined
    }
}

private fun ResultRow.toAccount(): Account {
    return Account(
        id = this[AccountsTable.id],
        fname = if (this.hasValue(AccountsTable.fname)) this[AccountsTable.fname] else null,
        sname = if (this.hasValue(AccountsTable.sname)) this[AccountsTable.sname] else null,
        email = this[AccountsTable.email],
        interests = null, //this[AccountsTable.interests],
        status = if (this.hasValue(AccountsTable.status)) StatusDbMapper.fromDbValue(this[AccountsTable.status]) else null,
        premium = if (this.hasValue(AccountsTable.premium_start) && this[AccountsTable.premium_start] != null) Premium(this[AccountsTable.premium_start]!!, this[AccountsTable.premium_finish]!!) else null, // this[AccountsTable.premium],
        sex = if (this.hasValue(AccountsTable.sex)) SexDbMapper.fromDbValue(this[AccountsTable.sex]) else null,
        phone = if (this.hasValue(AccountsTable.phone)) this[AccountsTable.phone] else null,
        likes = null, //this[AccountsTable.likes],
        birth = if (this.hasValue(AccountsTable.birth)) this[AccountsTable.birth] else null,
        city = if (this.hasValue(AccountsTable.city)) this[AccountsTable.city] else null,
        country = if (this.hasValue(AccountsTable.country)) this[AccountsTable.country] else null,
        joined = if (this.hasValue(AccountsTable.joined)) this[AccountsTable.joined] else null
    )
}


private fun ResultRow.toGroup(): Group {
    return Group(
        sex = if (this.hasValue(AccountsTable.sex)) SexDbMapper.fromDbValue(this[AccountsTable.sex]) else null,
        status = if (this.hasValue(AccountsTable.status)) StatusDbMapper.fromDbValue(this[AccountsTable.status]) else null,
        country = if (this.hasValue(AccountsTable.country)) this[AccountsTable.country] else null,
        city = if (this.hasValue(AccountsTable.city)) this[AccountsTable.city] else null,
        count = this[AccountsTable.id.count()]
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
private fun illegalArgument(): Nothing = throw IllegalArgumentException()
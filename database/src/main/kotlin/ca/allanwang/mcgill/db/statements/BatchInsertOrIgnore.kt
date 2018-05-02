package ca.allanwang.mcgill.db.statements

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.isAutoInc
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

class BatchInsertOrIgnore(table: Table) : BatchInsertStatement(table, false) {
    override fun prepareSQL(transaction: Transaction): String =
            PostgresStatements.ignore(super.prepareSQL(transaction))
}

fun <T : Table, E> T.batchInsertOrIgnore(data: List<E>, body: BatchInsertOrIgnore.(E) -> Unit): List<Int> {
    return data.takeIf { it.isNotEmpty() }?.let {
        val insert = BatchInsertOrIgnore(this)
        data.forEach {
            insert.addBatch()
            body(insert, it)
        }
        TransactionManager.current().exec(insert)
        columns.firstOrNull { it.columnType.isAutoInc }?.let { idCol ->
            insert.generatedKey?.mapNotNull {
                val value = it[idCol]
                when (value) {
                    is Long -> value.toInt()
                    is Int -> value
                    null -> null
                    else -> error("can't find primary key of type Int or Long; map['$idCol']='$value' (where map='$it')")
                }
            }
        }
    }.orEmpty()
}
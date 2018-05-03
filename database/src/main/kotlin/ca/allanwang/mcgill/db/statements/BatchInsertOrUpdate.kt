package ca.allanwang.mcgill.db.statements

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.isAutoInc
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

class BatchInsertUpdateOnDuplicate(table: Table,
                                   private val onDupUpdate: List<Column<*>>) : BatchInsertStatement(table, false) {
    override fun prepareSQL(transaction: Transaction): String =
            PostgresStatements.update(super.prepareSQL(transaction),
                    onDupUpdate,
                    table.columns,
                    transaction)
}

fun <T : Table, E> T.batchInsertOnDuplicateKeyUpdate(data: List<E>, onDupUpdateColumns: List<Column<*>>, body: BatchInsertUpdateOnDuplicate.(E) -> Unit): List<Int> {
    return data.takeIf { it.isNotEmpty() }?.let {
        val insert = BatchInsertUpdateOnDuplicate(this, onDupUpdateColumns)
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
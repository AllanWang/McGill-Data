package ca.allanwang.mcgill.db.statements

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T : Table> T.insertOrUpdate(onDupUpdate: List<Column<*>>, body: T.(InsertStatement<Number>) -> Unit) =
        InsertOrUpdate<Number>(onDupUpdate, this).apply {
            body(this)
            execute(TransactionManager.current())
        }

class InsertOrUpdate<Key : Any>(private val onDupUpdate: List<Column<*>>,
                                table: Table,
                                isIgnore: Boolean = false) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String =
            PostgresStatements.update(super.prepareSQL(transaction), onDupUpdate, transaction)
}
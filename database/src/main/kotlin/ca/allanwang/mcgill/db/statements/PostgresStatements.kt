package ca.allanwang.mcgill.db.statements

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction

object PostgresStatements {

    const val ON_CONFLICT_IGNORE = "ON CONFLICT DO NOTHING"

    fun ignore(statement: String) = "$statement $ON_CONFLICT_IGNORE"

    fun onConflictUpdate(columns: List<Column<*>>, transaction: Transaction): String {
        if (columns.isEmpty())
            return ""
        val ids = columns.map { transaction.identity(it) }
        val fullId = ids.joinToString()
        val updater = ids.joinToString(postfix = ";") { "$it=EXCLUDED.$it" }
        return "ON CONFLICT ($fullId) DO UPDATE SET $updater"
    }

    fun update(statement: String, columns: List<Column<*>>, transaction: Transaction): String =
            "$statement ${onConflictUpdate(columns, transaction)}"

}
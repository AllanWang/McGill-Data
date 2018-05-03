package ca.allanwang.mcgill.db.statements

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Transaction

object PostgresStatements {

    const val ON_CONFLICT_IGNORE = "ON CONFLICT DO NOTHING"

    fun ignore(statement: String) = "$statement $ON_CONFLICT_IGNORE"

    fun onConflictUpdate(conflictColumns: List<Column<*>>,
                         updateColumns: List<Column<*>>,
                         transaction: Transaction): String {
        if (conflictColumns.isEmpty())
            return ""
        val fullId = conflictColumns.joinToString { transaction.identity(it) }
        val updater = updateColumns.map { transaction.identity(it) }
                .joinToString(postfix = ";") { "$it=EXCLUDED.$it" }
        return "ON CONFLICT ($fullId) DO UPDATE SET $updater"
    }

    fun update(statement: String,
               conflictColumns: List<Column<*>>,
               updateColumns: List<Column<*>>,
               transaction: Transaction): String =
            "$statement ${onConflictUpdate(conflictColumns, updateColumns, transaction)}"

}
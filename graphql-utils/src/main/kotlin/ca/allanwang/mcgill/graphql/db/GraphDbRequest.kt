package ca.allanwang.mcgill.graphql.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class GraphDbRequest(val conditions: List<Op<Boolean>>,
                     val extensions: List<Query.() -> Query>,
                     val fields: List<Column<*>>) {

    fun retrieve(): Map<String, Any?> = transaction {
        val where = conditions.fold<Op<Boolean>, Op<Boolean>?>(null) {
            base, op ->
            if (base == null) op
            else base and op
        }
        emptyMap()
    }

}
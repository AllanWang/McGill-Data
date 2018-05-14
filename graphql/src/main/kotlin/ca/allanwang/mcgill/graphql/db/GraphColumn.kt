package ca.allanwang.mcgill.graphql.db

import ca.allanwang.mcgill.db.bindings.toCamel
import org.jetbrains.exposed.sql.Column

/**
 * Column wrapper with other fields relevant to the graphql creation
 */
class GraphColumn(val name: String, val column: Column<*>) {

    constructor(column: Column<*>) : this(column.name.toCamel(), column)

    val columnName: String
        get() = column.name

    val nullable: Boolean
        get() = column.columnType.nullable
}
package ca.allanwang.mcgill.graphql.db

import ca.allanwang.mcgill.db.utils.toCamel
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import org.jetbrains.exposed.sql.Column

/**
 * Column wrapper with other fields relevant to the graphql creation
 */
class GraphDbField(val name: String, val column: Column<*>, val description: String? = null) {

    constructor(column: Column<*>) : this(column.name.toCamel(), column, "Query for exact match with ${column.name}")

    val columnName: String
        get() = column.name

    val nullable: Boolean
        get() = column.columnType.nullable

    fun graphQLField() = graphQLFieldDefinition {
        name(name)
        description(description)
        type(TableWiring.outputType(column))
    }

}
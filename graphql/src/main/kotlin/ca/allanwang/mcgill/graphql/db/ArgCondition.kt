package ca.allanwang.mcgill.graphql.db

import ca.allanwang.mcgill.graphql.kotlin.graphQLArgument
import ca.allanwang.mcgill.graphql.kotlin.toCamel
import graphql.schema.GraphQLInputType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryParameter

/**
 * A link between an argument supplier and handler
 */
abstract class ArgCondition(val name: String,
                            private val type: GraphQLInputType,
                            private val description: String? = null) {

    fun buildArgument() = graphQLArgument(name, type, description)

    abstract fun handle(query: Any): Op<Boolean>?
}

class ArgEq(private val column: Column<*>) : ArgCondition(
        column.name.toCamel(),
        TableWiring.graphQLType(column) as GraphQLInputType,
        "Query for exact match with ${column.name}"
) {
    override fun handle(query: Any): Op<Boolean>? =
            EqOp(column, QueryParameter(query, column.columnType))

    companion object {
        fun with(vararg column: Column<*>) = column.map(::ArgEq)
    }
}
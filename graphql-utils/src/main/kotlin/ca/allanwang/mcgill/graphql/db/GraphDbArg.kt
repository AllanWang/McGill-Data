package ca.allanwang.mcgill.graphql.db

import ca.allanwang.mcgill.db.utils.toCamel
import ca.allanwang.mcgill.graphql.kotlin.graphQLArgument
import graphql.schema.GraphQLInputType
import org.jetbrains.exposed.sql.*

/**
 * SQL query wrapper with other fields relevant to the graphql creation
 */
sealed class GraphDbArg(val name: String,
                        val type: GraphQLInputType,
                        val description: String?) {

    fun graphQLArg() = graphQLArgument {
        name(name)
        description(description)
        type(type)
    }
}

fun Collection<GraphDbArg>.toMap() = map { it.name to it }.toMap()

class GraphDbConditionArg(name: String,
                          type: GraphQLInputType,
                          val where: (arg: String) -> Op<Boolean>,
                          default: Any? = null,
                          description: String? = null) : GraphDbArg(name, type, description) {

    constructor(name: String, column: Column<*>) : this(name,
            FieldDbWiring.inputType(column),
            { EqOp(column, QueryParameter(it, column.columnType)) })

    constructor(column: Column<*>) : this(column.name.toCamel(), column)

    private val default = default?.toString()

    fun call(arg: Any?): Op<Boolean>? {
        val input = arg?.toString() ?: default ?: return null
        return where(input)
    }

    companion object {
        fun fold(conditions: Collection<Op<Boolean>?>) = conditions.fold<Op<Boolean>?, Op<Boolean>?>(null) { acc, op ->
            when {
                op == null -> acc
                acc == null -> op
                else -> acc and op
            }
        }
    }
}

class GraphDbExtensionArg(name: String,
                          type: GraphQLInputType,
                          private val modifier: Query.(arg: String) -> Query,
                          default: Any? = null,
                          description: String?) : GraphDbArg(name, type, description) {

    private val default = default?.toString()

    fun call(query: Query, arg: Any?): Query {
        val input: String = arg?.toString() ?: default ?: return query
        return query.modifier(input)
    }
}
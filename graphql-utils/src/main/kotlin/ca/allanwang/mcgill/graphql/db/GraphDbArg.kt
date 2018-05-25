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

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean =
            this === other || (other is GraphDbArg && name == other.name)

    override fun toString(): String = "${this::class.java.simpleName}: $name"
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

        /**
         * Converts a map of argument data to a single op boolean, or null if none are supplied
         */
        fun fold(data: Map<GraphDbConditionArg, String>, initial: Op<Boolean>? = null) = data.entries.fold(initial) {
            acc, (arg, value) ->
            val op = arg.call(value)
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

    companion object {

        /**
         * Converts a map of argument data to a single op boolean, or null if none are supplied
         */
        fun fold(data: Map<GraphDbExtensionArg, String>, initial: Query) : Query = data.entries.fold(initial) {
            acc, (arg, value) -> arg.call(acc, value)
        }

    }
}
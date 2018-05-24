package ca.allanwang.mcgill.graphql.db

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

class GraphDbConditionArg(name: String,
                          type: GraphQLInputType,
                          val where: (arg: Any) -> Op<Boolean>,
                          description: String? = null) : GraphDbArg(name, type, description) {
    constructor(name: String, column: Column<*>) : this(name,
            TableWiring.inputType(column),
            { EqOp(column, QueryParameter(it, column.columnType)) })

}

class GraphDbExtensionArg(name: String,
                          type: GraphQLInputType,
                          val modifier: Query.(arg: String) -> Query,
                          description: String?) : GraphDbArg(name, type, description)

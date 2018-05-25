package ca.allanwang.mcgill.graphql.db

import ca.allanwang.mcgill.db.utils.toCamel
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import graphql.Scalars
import graphql.schema.GraphQLOutputType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Column

/**
 * Column wrapper with other fields relevant to the graphql creation
 */
class GraphDbColField(name: String, val column: Column<*>, description: String? = null)
    : GraphDbField(name, FieldDbWiring.outputType(column), description) {

    constructor(column: Column<*>) : this(column.name.toCamel(), column, "Query for exact match with ${column.name}")

}

class GraphDbRetrievalField<ID : Comparable<ID>, E : Entity<ID>>(name: String, val retrieve: (entity: E) -> Any?, description: String?)
    :GraphDbField(name, Scalars.GraphQLString, description){ // todo update type

}

open class GraphDbField(val name: String,
                        val type: GraphQLOutputType,
                        val description: String? = null) {
    fun graphQLField() = graphQLFieldDefinition {
        name(name)
        description(description)
        type(type)
    }
}

fun <T : GraphDbField> Collection<T>.toMap() = map { it.name to it }.toMap()
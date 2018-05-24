package ca.allanwang.mcgill.graphql.db

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeReference
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper class for generating graphql related data
 */
open class GraphQLWiring(private vararg val wirings: FieldDbWiring<*>) {

    fun schema() = GraphQLSchema.newSchema()
            .query(GraphQLObjectType.newObject()
                    .name("query")
                    .fields(fields())
                    .build())
            .build()

    private val typeMapper: MutableMap<Table, GraphQLObjectType> = mutableMapOf()

    /**
     * Returns the full object type of an object reference
     */
    fun type(fieldWiring: FieldDbWiring<*>): GraphQLOutputType {
        val existingType = typeMapper[fieldWiring.table]
        if (existingType != null) return GraphQLTypeReference(existingType.name)
        val type = fieldWiring.objectTypeFactory()
        typeMapper[fieldWiring.table] = type
        return type
    }

    private fun fields() = transaction {
        typeMapper.clear()
        val fields = wirings.map { it.field(this@GraphQLWiring) }
        typeMapper.clear()
        fields
    }
}